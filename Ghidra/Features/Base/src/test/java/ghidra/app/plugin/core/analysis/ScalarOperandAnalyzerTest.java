/* ###
 * IP: GHIDRA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ghidra.app.plugin.core.analysis;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.*;

import generic.test.AbstractGenericTest;
import ghidra.program.database.ProgramBuilder;
import ghidra.program.database.ProgramDB;
import ghidra.program.model.listing.Instruction;
import ghidra.program.model.listing.Program;
import ghidra.program.model.reloc.Relocation.Status;
import ghidra.program.model.reloc.RelocationTable;
import ghidra.program.model.symbol.Reference;

public class ScalarOperandAnalyzerTest extends AbstractGenericTest {

	private ProgramBuilder builder;
	private ProgramDB program;
	private ScalarOperandAnalyzer analyzer;

	@Before
	public void setUp() throws Exception {
		builder = new ProgramBuilder("ScalarOperandAnalyzerTest", ProgramBuilder._X86);
		builder.createMemory(".text", "0x1000", 0x100);
		builder.createMemory(".data", "0x0", 0x1000);
		program = builder.getProgram();
		analyzer = new ScalarOperandAnalyzer();
	}

	@After
	public void tearDown() {
		builder.dispose();
	}

	@Test
	public void testSmallScalarWithoutRelocationsIsIgnored() throws Exception {
		Instruction instruction = instruction("b8 80 00 00 00"); // MOV EAX,0x80
		analyze(instruction);
		assertEquals(0, instruction.getOperandReferences(1).length);
	}

	@Test
	public void testLargeScalarWithoutRelocationsCreatesReference() throws Exception {
		Instruction instruction = instruction("b8 80 10 00 00"); // MOV EAX,0x1080
		analyze(instruction);
		assertReference(instruction, 1, "0x1080");
	}

	@Test
	public void testMatchingRelocationCreatesReference() throws Exception {
		Instruction instruction = instruction("b8 80 00 00 00");
		addRelocation("0x1001");
		analyze(instruction);
		assertReference(instruction, 1, "0x80");
	}

	@Test
	public void testMismatchedRelocationDoesNotCreateReference() throws Exception {
		Instruction instruction = instruction("b8 80 00 00 00");
		addRelocation("0x1002");
		analyze(instruction);
		assertEquals(0, instruction.getOperandReferences(1).length);
	}

	@Test
	public void testRelocationsOutsideInstructionAreIgnored() throws Exception {
		Instruction instruction = instruction("b8 80 00 00 00");
		builder.setBytes("0x1005", "80 00 00 00");
		addRelocation("0xfff");
		addRelocation("0x1005");
		analyze(instruction);
		assertEquals(0, instruction.getOperandReferences(1).length);
	}

	@Test
	public void testRelocationAtInstructionStartIsIncluded() throws Exception {
		// The opcode byte equals the immediate, so a relocation at the first byte matches.
		Instruction instruction = instruction("b0 b0"); // MOV AL,0xb0
		addRelocation("0x1000");
		analyze(instruction);
		assertReference(instruction, 1, "0xb0");
	}

	@Test
	public void testMultipleScalarsAndRelocationAtLastByte() throws Exception {
		Instruction instruction = instruction("c8 80 00 04"); // ENTER 0x80,0x4
		addRelocation("0x1001");
		addRelocation("0x1003");
		analyze(instruction);
		assertReference(instruction, 0, "0x80");
		assertReference(instruction, 1, "0x4");
	}

	@Test
	public void testDuplicateRelocationsDoNotDuplicateReferences() throws Exception {
		Instruction instruction = instruction("b8 80 00 00 00");
		addRelocation("0x1001");
		addRelocation("0x1001");
		analyze(instruction);
		assertReference(instruction, 1, "0x80");
	}

	@Test
	public void testLaterInvocationSeesNewRelocation() throws Exception {
		Instruction instruction = instruction("b8 80 00 00 00");
		analyze(instruction);
		assertEquals(0, instruction.getOperandReferences(1).length);
		addRelocation("0x1001");
		analyze(instruction);
		assertReference(instruction, 1, "0x80");
	}

	@Test
	public void testEmptyRelocationTableRequiresNoAddressLookups() throws Exception {
		Instruction instruction = instruction("c8 80 00 04");
		assertEquals(0, countRelocationLookups(instruction));
	}

	@Test
	public void testSparseRelocationsRequireAtMostTwoLookupsPerInstruction() throws Exception {
		Instruction instruction = instruction("c8 80 00 04");
		addRelocation("0x1080");
		assertTrue(countRelocationLookups(instruction) <= 2);
	}

	private int countRelocationLookups(Instruction instruction) throws Exception {
		AtomicInteger lookups = new AtomicInteger();
		RelocationTable table = program.getRelocationTable();
		RelocationTable countingTable = (RelocationTable) Proxy.newProxyInstance(
			RelocationTable.class.getClassLoader(), new Class<?>[] { RelocationTable.class },
			(proxy, method, args) -> {
				if (method.getName().equals("hasRelocation") ||
					method.getName().equals("getRelocationAddressAfter")) {
					lookups.incrementAndGet();
				}
				try {
					return method.invoke(table, args);
				}
				catch (InvocationTargetException e) {
					throw e.getCause();
				}
			});
		Program countingProgram = (Program) Proxy.newProxyInstance(Program.class.getClassLoader(),
			new Class<?>[] { Program.class }, (proxy, method, args) -> {
				if (method.getName().equals("getRelocationTable")) {
					return countingTable;
				}
				try {
					return method.invoke(program, args);
				}
				catch (InvocationTargetException e) {
					throw e.getCause();
				}
			});
		builder.withTransaction(() -> analyzer.checkOperands(countingProgram, instruction));
		return lookups.get();
	}

	private Instruction instruction(String bytes) throws Exception {
		builder.setBytes("0x1000", bytes);
		builder.disassemble("0x1000", bytes.split(" ").length);
		return program.getListing().getInstructionAt(builder.addr("0x1000"));
	}

	private void addRelocation(String address) throws Exception {
		builder.withTransaction(() -> program.getRelocationTable()
				.add(builder.addr(address), Status.UNKNOWN, 0, null, new byte[] { 0 }, null));
	}

	private void analyze(Instruction instruction) throws Exception {
		builder.withTransaction(() -> analyzer.checkOperands(program, instruction));
	}

	private void assertReference(Instruction instruction, int operand, String target) {
		Reference[] references = instruction.getOperandReferences(operand);
		assertEquals(1, references.length);
		assertEquals(builder.addr(target), references[0].getToAddress());
	}
}
