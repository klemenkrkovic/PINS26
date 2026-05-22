package pins26.phase;

import java.util.*;

import pins26.common.*;

/**
 * Generiranje kode.
 */
public class CodeGen {

	@SuppressWarnings({ "doclint:missing" })
	public CodeGen() {
		throw new Report.InternalError();
	}

	/**
	 * Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
	 * predstavitve.
	 * 
	 * Atributi:
	 * <ol>
	 * <li>({@link Abstr}) lokacija kode, ki pripada posameznemu vozliscu;</li>
	 * <li>({@link SemAn}) definicija uporabljenega imena;</li>
	 * <li>({@link SemAn}) ali je dani izraz levi izraz;</li>
	 * <li>({@link Memory}) klicni zapis funkcije;</li>
	 * <li>({@link Memory}) dostop do parametra;</li>
	 * <li>({@link Memory}) dostop do spremenljivke;</li>
	 * <li>({@link CodeGen}) seznam ukazov, ki predstavljajo kodo programa;</li>
	 * <li>({@link CodeGen}) seznam ukazov, ki predstavljajo podatke programa.</li>
	 * </ol>
	 */
	public static class AttrAST extends Memory.AttrAST {

		/** Atribut: seznam ukazov, ki predstavljajo kodo programa. */
		public final Map<AST.Node, List<PDM.CodeInstr>> attrCode;

		/** Atribut: seznam ukazov, ki predstavljajo podatke programa. */
		public final Map<AST.Node, List<PDM.DataInstr>> attrData;

		/**
		 * Ustvari novo abstraktno sintaksno drevo z dodanimi atributi generiranja kode.
		 * 
		 * @param attrAST  Abstraktno sintaksno drevo z dodanimi atributi pomnilniske
		 *                 predstavitve.
		 * @param attrCode Attribut: seznam ukazov, ki predstavljajo kodo programa.
		 * @param attrData Attribut: seznam ukazov, ki predstavljajo podatke programa.
		 */
		public AttrAST(final Memory.AttrAST attrAST, final Map<AST.Node, List<PDM.CodeInstr>> attrCode,
				final Map<AST.Node, List<PDM.DataInstr>> attrData) {
			super(attrAST);
			this.attrCode = attrCode;
			this.attrData = attrData;
		}

		/**
		 * Ustvari novo abstraktno sintaksno drevo z dodanimi atributi generiranja kode.
		 * 
		 * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi generiranja
		 *                kode.
		 */
		public AttrAST(final AttrAST attrAST) {
			super(attrAST);
			this.attrCode = attrAST.attrCode;
			this.attrData = attrAST.attrData;
		}

		@Override
		public String head(final AST.Node node, final boolean highlighted) {
			final StringBuffer head = new StringBuffer();
			head.append(super.head(node, false));
			return head.toString();
		}

		@Override
		public void desc(final int indent, final AST.Node node, final boolean highlighted) {
			super.desc(indent, node, false);
			System.out.print(highlighted ? "\033[31m" : "");
			if (attrCode.get(node) != null) {
				List<PDM.CodeInstr> instrs = attrCode.get(node);
				if (instrs != null) {
					if (indent > 0)
						System.out.printf("%" + indent + "c", ' ');
					System.out.printf("--- Code: ---\n");
					for (final PDM.CodeInstr instr : instrs) {
						if (indent > 0)
							System.out.printf("%" + indent + "c", ' ');
						System.out.println((instr instanceof PDM.LABEL ? "" : "  ") + instr.toString());
					}
				}
			}
			if (attrData.get(node) != null) {
				List<PDM.DataInstr> instrs = attrData.get(node);
				if (instrs != null) {
					if (indent > 0)
						System.out.printf("%" + indent + "c", ' ');
					System.out.printf("--- Data: ---\n");
					for (final PDM.DataInstr instr : instrs) {
						if (indent > 0)
							System.out.printf("%" + indent + "c", ' ');
						System.out.println((instr instanceof PDM.LABEL ? "" : "  ") + instr.toString());
					}
				}
			}
			System.out.print(highlighted ? "\033[30m" : "");
			return;
		}

	}

	/**
	 * Izracuna kodo programa
	 * 
	 * @param memoryAttrAST Abstraktno sintaksno drevo z dodanimi atributi izracuna
	 *                      pomnilniske predstavitve.
	 * @return Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
	 *         predstavitve.
	 */
	public static AttrAST generate(final Memory.AttrAST memoryAttrAST) {
		AttrAST attrAST = new AttrAST(memoryAttrAST, new HashMap<AST.Node, List<PDM.CodeInstr>>(),
				new HashMap<AST.Node, List<PDM.DataInstr>>());
		(new CodeGenerator(attrAST)).generate();
		return attrAST;
	}

	/**
	 * Generiranje kode v abstraktnem sintaksnem drevesu.
	 */
	private static class CodeGenerator {

		/**
		 * Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
		 * predstavitve.
		 */
		private final AttrAST attrAST;

		/** Stevec anonimnih label. */
		private int labelCounter = 0;

		/**
		 * Ustvari nov generator kode v abstraktnem sintaksnem drevesu.
		 * 
		 * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi izracuna
		 *                pomnilniske predstavitve.
		 */
		public CodeGenerator(final AttrAST attrAST) {
			this.attrAST = attrAST;
		}

		/**
		 * Sprozi generiranje kode v abstraktnem sintaksnem drevesu.
		 * 
		 * @return Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
		 *         predstavitve.
		 */
		public AttrAST generate() {
			attrAST.ast.accept(new Generator(), null);
			return new AttrAST(attrAST, Collections.unmodifiableMap(attrAST.attrCode),
					Collections.unmodifiableMap(attrAST.attrData));
		}

		/** Obiskovalec, ki generira kodo v abstraktnem sintaksnem drevesu. */
		private class Generator implements AST.FullVisitor<List<PDM.CodeInstr>, Mem.Frame> {

			@SuppressWarnings({ "doclint:missing" })
			public Generator() {
			}

			public static PDM.OPER.Oper getMachineOper(AST.BinExpr.Oper oper) {
				return switch (oper) {
					case OR -> PDM.OPER.Oper.OR;
					case AND -> PDM.OPER.Oper.AND;
					case EQU -> PDM.OPER.Oper.EQU;
					case NEQ -> PDM.OPER.Oper.NEQ;
					case GTH -> PDM.OPER.Oper.GTH;
					case LTH -> PDM.OPER.Oper.LTH;
					case GEQ -> PDM.OPER.Oper.GEQ;
					case LEQ -> PDM.OPER.Oper.LEQ;
					case ADD -> PDM.OPER.Oper.ADD;
					case SUB -> PDM.OPER.Oper.SUB;
					case MUL -> PDM.OPER.Oper.MUL;
					case DIV -> PDM.OPER.Oper.DIV;
					case MOD -> PDM.OPER.Oper.MOD;
				};
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.Nodes<? extends AST.Node> nodes, Mem.Frame arg) {
				Vector<PDM.CodeInstr> codeInstrs = new Vector<>();
				for (AST.Node node : nodes) {

					final List<PDM.CodeInstr> instrs = node.accept(this, arg);
					if (instrs == null) {
						System.out.println("Warning: empty code instruction list for " + node);
						continue;
					}
					codeInstrs.addAll(instrs);
				}
				return codeInstrs;
			}
			@Override
			public List<PDM.CodeInstr> visit(AST.AtomExpr atomExpr, Mem.Frame arg) {
				Vector<PDM.CodeInstr> instructions = new Vector<>();
				Report.Locatable loc = attrAST.attrLoc.get(atomExpr);
				switch (atomExpr.type) {
					case INTCONST -> instructions.add(new PDM.PUSH(Memory.decodeIntConst(atomExpr, loc), loc));
					case CHRCONST -> instructions.add(new PDM.PUSH(Memory.decodeChrConst(atomExpr, loc), loc));
					case STRCONST -> {
						Vector<PDM.DataInstr> dataInstructions = new Vector<>();
						String label = ":" + labelCounter++;
						dataInstructions.add(new PDM.LABEL(label, loc));
						Memory.decodeStrConst(atomExpr, loc).forEach(c -> dataInstructions.add(new PDM.DATA(c, loc)));
						attrAST.attrData.put(atomExpr, dataInstructions);
						instructions.add(new PDM.NAME(label, loc));
					}
				}
				attrAST.attrCode.put(atomExpr, instructions);
				return instructions;
			}
			@Override
			public List<PDM.CodeInstr> visit(AST.BinExpr binExpr, Mem.Frame arg) {
				Vector<PDM.CodeInstr> instructions = new Vector<>();
				instructions.addAll(binExpr.fstExpr.accept(this, arg));
				instructions.addAll(binExpr.sndExpr.accept(this, arg));
				instructions.add(new PDM.OPER(getMachineOper(binExpr.oper), attrAST.attrLoc.get(binExpr)));
				attrAST.attrCode.put(binExpr, instructions);
				return instructions;
			}
			@Override
			public List<PDM.CodeInstr> visit(AST.UnExpr unExpr, Mem.Frame arg) {
				Vector<PDM.CodeInstr> instructions = new Vector<>(unExpr.expr.accept(this, arg));
				Report.Locatable loc = attrAST.attrLoc.get(unExpr);
				switch (unExpr.oper) {
					case SUB -> instructions.add(new PDM.OPER(PDM.OPER.Oper.NEG, loc));
					case NOT -> instructions.add(new PDM.OPER(PDM.OPER.Oper.NOT, loc));
					case MEMADDR -> {
						if (unExpr.expr instanceof AST.VarExpr varExpr) {
							Vector<PDM.CodeInstr> varInstructions = (Vector<PDM.CodeInstr>) varExpr.accept(this, arg);
							// Check if varExpr is a local variable
							if (!varInstructions.isEmpty() && varInstructions.getLast() instanceof PDM.LOAD)
								varInstructions.removeLast();
							else
								throw new Report.Error(loc, "Tried to get adress for %s, last instruction is not LOAD".formatted(varExpr.name));
							instructions.addAll(varInstructions);
						} else {
							throw new Report.Error(loc, "Tried to get adress for %s, but it is not a variable".formatted(unExpr.expr));
						}
					}
					case VALUEAT -> instructions.add(new PDM.LOAD(loc));
				}
				return instructions;
			}
			@Override
			public List<PDM.CodeInstr> visit(AST.VarExpr varExpr, Mem.Frame arg) {
				Vector<PDM.CodeInstr> instructions = new Vector<>();
				Report.Locatable loc = attrAST.attrLoc.get(varExpr);
				// Check if parameter or local variable

				AST.Def def = attrAST.attrDef.get(varExpr);
				Mem.Access access = def instanceof AST.VarDef ?
						attrAST.attrVarAccess.get(def) : attrAST.attrParAccess.get((AST.ParDef) def);

				if (access instanceof Mem.AbsAccess absAccess) {
					instructions.add(new PDM.NAME(absAccess.name, loc));

					if (access.size == 4 && access.inits != null && !access.inits.isEmpty()) {
						instructions.add(new PDM.LOAD(loc));
					}
				} else if (access instanceof Mem.RelAccess relAccess) {
					instructions.add(new PDM.REGN(PDM.REGN.Reg.FP, loc));
					// This adds loads for each level of depth
					for (int i = 0; i < arg.depth - relAccess.depth; i++)
						instructions.add(new PDM.LOAD(loc));

					instructions.add(new PDM.PUSH(relAccess.offset, loc));
					instructions.add(new PDM.OPER(PDM.OPER.Oper.ADD, loc));

					if (attrAST.attrParAccess.containsValue(relAccess)
							|| access.size == 4 && relAccess.inits != null && !relAccess.inits.isEmpty()) {
						instructions.add(new PDM.LOAD(loc));
					}
				}
				attrAST.attrCode.put(varExpr, instructions);
				return instructions;
			}
			/* TODO: change the part that just gets rid of last LOAD to be a little better */
			@Override
			public List<PDM.CodeInstr> visit(AST.AssignStmt assignStmt, Mem.Frame arg) {
				Vector<PDM.CodeInstr> instructions = new Vector<>();
				Report.Locatable loc = attrAST.attrLoc.get(assignStmt);
				instructions.addAll(assignStmt.srcExpr.accept(this, arg));
				instructions.addAll(assignStmt.dstExpr.accept(this, arg));
				if (instructions.getLast() instanceof PDM.LOAD) {
					instructions.removeLast();
				}
				instructions.add(new PDM.SAVE(loc));
				attrAST.attrCode.put(assignStmt, instructions);
				return instructions;
			}
			@Override
			public List<PDM.CodeInstr> visit(AST.ExprStmt exprStmt, Mem.Frame arg) {
				Vector<PDM.CodeInstr> instructions = new Vector<>(exprStmt.expr.accept(this, arg));
				Report.Locatable loc = attrAST.attrLoc.get(exprStmt);
				instructions.add(new PDM.PUSH(4, loc));
				instructions.add(new PDM.POPN(loc));
				return instructions;
			}
			@Override
			public List<PDM.CodeInstr> visit(AST.IfStmt ifStmt, Mem.Frame arg) {
				Report.Locatable loc = attrAST.attrLoc.get(ifStmt);
				final String ifStatementLabel = ":if:" + ++labelCounter;
				final String elseStatementLabel = ":else:" + labelCounter;
				final String endStatementLabel = ":end:" + labelCounter;
				Vector<PDM.CodeInstr> instructions = new Vector<>(ifStmt.cond.accept(this, arg));
				instructions.add(new PDM.NAME(ifStatementLabel, loc));
				if (ifStmt.elseStmts != null && ifStmt.elseStmts.size() > 0) {
					instructions.add(new PDM.NAME(elseStatementLabel, loc));
				} else {
					instructions.add(new PDM.NAME(endStatementLabel, loc));
				}
				instructions.add(new PDM.CJMP(loc));
				// if statements
				instructions.add(new PDM.LABEL(ifStatementLabel, loc));
				instructions.addAll(ifStmt.thenStmts.accept(this, arg));
				if (ifStmt.elseStmts != null && ifStmt.elseStmts.size() > 0) {
					instructions.add(new PDM.NAME(endStatementLabel, loc));
					instructions.add(new PDM.UJMP(loc));
					// else statements
					instructions.add(new PDM.LABEL(elseStatementLabel, loc));
					instructions.addAll(ifStmt.elseStmts.accept(this, arg));
				}
				// End
				instructions.add(new PDM.LABEL(endStatementLabel, loc));
				return instructions;
			}
			@Override
			public List<PDM.CodeInstr> visit(AST.WhileStmt whileStmt, Mem.Frame arg) {
				Vector<PDM.CodeInstr> instructions = new Vector<>();
				Report.Locatable loc = attrAST.attrLoc.get(whileStmt);
				final String whileStatementLabel = ":while_cond:" + ++labelCounter;
				final String whileBodyLabel = ":while_body:" + labelCounter;
				final String endStatementLabel = ":end:" + labelCounter;
				// Condition
				instructions.add(new PDM.LABEL(whileStatementLabel, loc));
				instructions.addAll(whileStmt.cond.accept(this, arg));
				instructions.add(new PDM.NAME(whileBodyLabel, loc));
				instructions.add(new PDM.NAME(endStatementLabel, loc));
				instructions.add(new PDM.CJMP(loc));
				// Body
				instructions.add(new PDM.LABEL(whileBodyLabel, loc));
				instructions.addAll(whileStmt.stmts.accept(this, arg));
				instructions.add(new PDM.NAME(whileStatementLabel, loc));
				instructions.add(new PDM.UJMP(loc));
				// End
				instructions.add(new PDM.LABEL(endStatementLabel, loc));
				return instructions;
			}
			@Override
			public List<PDM.CodeInstr> visit(AST.LetStmt letStmt, Mem.Frame arg) {
				Vector<PDM.CodeInstr> instructions = new Vector<>();
				for (AST.MainDef def : letStmt.defs) {
					if (def instanceof AST.VarDef)
						instructions.addAll(def.accept(this, arg));
					else
						def.accept(this, arg);
				}
				instructions.addAll(letStmt.stmts.accept(this, arg));
				return instructions;
			}
			@Override
			public List<PDM.CodeInstr> visit(AST.CallExpr callExpr, Mem.Frame arg) {
				Vector<PDM.CodeInstr> instructions = new Vector<>();
				Report.Locatable loc = attrAST.attrLoc.get(callExpr);
				AST.FunDef funDef = (AST.FunDef) attrAST.attrDef.get(callExpr);
				Report.Locatable funDefLoc = attrAST.attrLoc.get(funDef);
				Mem.Frame frame = attrAST.attrFrame.get(funDef);
				// Add arguments in reverse order so the first argument is on top of the stack
				for (int i = callExpr.args.size() - 1; i >= 0; i--) {
					instructions.addAll(callExpr.args.get(i).accept(this, arg));
				}
				if (frame.depth == 1) {
					instructions.add(new PDM.PUSH(0, loc));
				} else {
					instructions.add(new PDM.REGN(PDM.REGN.Reg.FP, loc));
					// This adds loads for each level of depth
					for (int i = 0; i < arg.depth - frame.depth + 1; i++)
						instructions.add(new PDM.LOAD(loc));
				}
				instructions.add(new PDM.NAME("main".equals(funDef.name) || funDef.stmts.size() == 0 ? funDef.name : funDef.name + ":" + funDefLoc.toString(), loc));
				instructions.add(new PDM.CALL(arg, loc));
				return instructions;
			}
			@Override
			public List<PDM.CodeInstr> visit(AST.FunDef funDef, Mem.Frame arg) {
				Vector<PDM.CodeInstr> instructions = new Vector<>();
				Report.Locatable loc = attrAST.attrLoc.get(funDef);
				Mem.Frame frame = attrAST.attrFrame.get(funDef);
				instructions.add(new PDM.LABEL("main".equals(funDef.name) || funDef.stmts.size() == 0 ? funDef.name : funDef.name + ":" + loc.toString(), loc));
				int varReserveSize = frame.varsSize - 8;
				// Optimization - only do POPN if there are local variables
				if (varReserveSize > 0) {
					instructions.add(new PDM.PUSH(varReserveSize * (-1), loc));
					instructions.add(new PDM.POPN(loc));
				}
				instructions.addAll(funDef.stmts.accept(this, frame));
				// Remove last popn instruction
				if (funDef.stmts.size() > 0) {
					instructions.removeLast();
					instructions.removeLast();
				}
				instructions.add(new PDM.PUSH(frame.parsSize - 4, loc));
				instructions.add(new PDM.RETN(frame, loc));
				attrAST.attrCode.put(funDef, instructions);
				return instructions;
			}
			@Override
			public List<PDM.CodeInstr> visit(AST.VarDef varDef, Mem.Frame arg) {
				Vector<PDM.CodeInstr> instructions = new Vector<>();
				Vector<PDM.DataInstr> dataInstructions = new Vector<>();
				Report.Locatable loc = attrAST.attrLoc.get(varDef);
				Mem.Access access = attrAST.attrVarAccess.get(varDef);
				if (access instanceof Mem.RelAccess relAccess) {
					final String label = ":" + labelCounter++;
					dataInstructions.add(new PDM.LABEL(label, loc));
					relAccess.inits.forEach(init -> dataInstructions.add(new PDM.DATA(init, loc)));
					instructions.add(new PDM.REGN(PDM.REGN.Reg.FP, loc));
					instructions.add(new PDM.PUSH(relAccess.offset, loc));
					instructions.add(new PDM.OPER(PDM.OPER.Oper.ADD, loc));
					instructions.add(new PDM.NAME(label, loc));
					instructions.add(new PDM.INIT(loc));
				} else if (access instanceof Mem.AbsAccess absAccess) {
					dataInstructions.add(new PDM.LABEL(absAccess.name, loc));
					dataInstructions.add(new PDM.SIZE(absAccess.size, loc));
					if (absAccess.inits != null && !absAccess.inits.isEmpty()) {
						final String label = ":" + labelCounter++;
						dataInstructions.add(new PDM.LABEL(label, loc));
						absAccess.inits.forEach(init -> dataInstructions.add(new PDM.DATA(init, loc)));
						instructions.add(new PDM.NAME(absAccess.name, loc));
						instructions.add(new PDM.NAME(label, loc));
						instructions.add(new PDM.INIT(loc));
					}
				}
				attrAST.attrCode.put(varDef, instructions);
				attrAST.attrData.put(varDef, dataInstructions);
				return instructions;
			}

			/* TODO */


		}

	}

	/**
	 * Generator seznama ukazov, ki predstavljajo kodo programa.
	 */
	public static class CodeSegmentGenerator {

		/**
		 * Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
		 * predstavitve.
		 */
		private final AttrAST attrAST;

		/** Seznam ukazov za inicializacijo staticnih spremenljivk. */
		private final Vector<PDM.CodeInstr> codeInitSegment = new Vector<PDM.CodeInstr>();

		/** Seznam ukazov funkcij. */
		private final Vector<PDM.CodeInstr> codeFunsSegment = new Vector<PDM.CodeInstr>();

		/** Klicni zapis funkcije {@code main}. */
		private Mem.Frame main = null;

		/**
		 * Ustvari nov generator seznama ukazov, ki predstavljajo kodo programa.
		 *
		 * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi izracuna
		 *                pomnilniske predstavitve.
		 */
		public CodeSegmentGenerator(final AttrAST attrAST) {
			this.attrAST = attrAST;
		}

		/**
		 * Izracuna seznam ukazov, ki predstavljajo kodo programa.
		 * 
		 * @return Seznam ukazov, ki predstavljajo kodo programa.
		 */
		public List<PDM.CodeInstr> codeSegment() {
			attrAST.ast.accept(new Generator(), null);
			codeInitSegment.addLast(new PDM.PUSH(0, null));
			codeInitSegment.addLast(new PDM.NAME("main", null));
			codeInitSegment.addLast(new PDM.CALL(main, null));
			codeInitSegment.addLast(new PDM.PUSH(0, null));
			codeInitSegment.addLast(new PDM.NAME("exit", null));
			codeInitSegment.addLast(new PDM.CALL(null, null));
			final Vector<PDM.CodeInstr> codeSegment = new Vector<PDM.CodeInstr>();
			codeSegment.addAll(codeInitSegment);
			codeSegment.addAll(codeFunsSegment);
			return Collections.unmodifiableList(codeSegment);
		}

		/**
		 * Obiskovalec, ki izracuna seznam ukazov, ki predstavljajo kodo programa.
		 */
		private class Generator implements AST.FullVisitor<Object, Object> {

			@SuppressWarnings({ "doclint:missing" })
			public Generator() {
			}

			@Override
			public Object visit(final AST.FunDef funDef, final Object arg) {
				if (funDef.stmts.size() == 0)
					return null;
				List<PDM.CodeInstr> code = attrAST.attrCode.get(funDef);
				codeFunsSegment.addAll(code);
				funDef.pars.accept(this, arg);
				funDef.stmts.accept(this, arg);
				switch (funDef.name) {
				case "main" -> main = attrAST.attrFrame.get(funDef);
				}
				return null;
			}

			@Override
			public Object visit(final AST.VarDef varDef, final Object arg) {
				switch (attrAST.attrVarAccess.get(varDef)) {
				case Mem.AbsAccess __: {
					List<PDM.CodeInstr> code = attrAST.attrCode.get(varDef);
					codeInitSegment.addAll(code);
					break;
				}
				case Mem.RelAccess __: {
					break;
				}
				default:
					throw new Report.InternalError();
				}
				return null;
			}

		}

	}

	/**
	 * Generator seznama ukazov, ki predstavljajo podatke programa.
	 */
	public static class DataSegmentGenerator {

		/**
		 * Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
		 * predstavitve.
		 */
		private final AttrAST attrAST;

		/** Seznam ukazov, ki predstavljajo podatke programa. */
		private final Vector<PDM.DataInstr> dataSegment = new Vector<PDM.DataInstr>();

		/**
		 * Ustvari nov generator seznama ukazov, ki predstavljajo podatke programa.
		 *
		 * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi izracuna
		 *                pomnilniske predstavitve.
		 */
		public DataSegmentGenerator(final AttrAST attrAST) {
			this.attrAST = attrAST;
		}

		/**
		 * Izracuna seznam ukazov, ki predstavljajo podatke programa.
		 * 
		 * @return Seznam ukazov, ki predstavljajo podatke programa.
		 */
		public List<PDM.DataInstr> dataSegment() {
			attrAST.ast.accept(new Generator(), null);
			return Collections.unmodifiableList(dataSegment);
		}

		/**
		 * Obiskovalec, ki izracuna seznam ukazov, ki predstavljajo podatke programa.
		 */
		private class Generator implements AST.FullVisitor<Object, Object> {

			@SuppressWarnings({ "doclint:missing" })
			public Generator() {
			}

			@Override
			public Object visit(final AST.VarDef varDef, final Object arg) {
				List<PDM.DataInstr> data = attrAST.attrData.get(varDef);
				if (data != null)
					dataSegment.addAll(data);
				varDef.inits.accept(this, arg);
				return null;
			}

			@Override
			public Object visit(final AST.AtomExpr atomExpr, final Object arg) {
				List<PDM.DataInstr> data = attrAST.attrData.get(atomExpr);
				if (data != null)
					dataSegment.addAll(data);
				return null;
			}

		}

	}

	// --- ZAGON ---

	/**
	 * Zagon izracuna pomnilniske predstavitve kot samostojnega programa.
	 * 
	 * @param cmdLineArgs Argumenti v ukazni vrstici.
	 */
	public static void main(final String[] cmdLineArgs) {
		System.out.println("This is PINS'26 compiler (code generation):");

		try {
			if (cmdLineArgs.length == 0)
				throw new Report.Error("No source file specified in the command line.");
			if (cmdLineArgs.length > 1)
				Report.warning("Unused arguments in the command line.");

			try (SynAn synAn = new SynAn(cmdLineArgs[0])) {
				// abstraktna sintaksa:
				final Abstr.AttrAST abstrAttrAST = Abstr.constructAST(synAn);
				// semanticna analiza:
				final SemAn.AttrAST semanAttrAST = SemAn.analyze(abstrAttrAST);
				// pomnilniska predstavitev:
				final Memory.AttrAST memoryAttrAST = Memory.organize(semanAttrAST);
				// generiranje kode:
				final CodeGen.AttrAST codegenAttrAST = CodeGen.generate(memoryAttrAST);

				(new AST.Logger(codegenAttrAST)).log();
				{
					int addr = 0;
					final List<PDM.CodeInstr> codeSegment = (new CodeSegmentGenerator(codegenAttrAST)).codeSegment();
					{
						System.out.println("\n\033[1mCODE SEGMENT:\033[0m");
						for (final PDM.CodeInstr instr : codeSegment) {
							System.out.printf("%8d [%s] %s\n", addr, instr.size(),
									(instr instanceof PDM.LABEL ? "" : "  ") + instr.toString());
							addr += instr.size();
						}
					}
					final List<PDM.DataInstr> dataSegment = (new DataSegmentGenerator(codegenAttrAST)).dataSegment();
					{
						System.out.println("\n\033[1mDATA SEGMENT:\033[0m");
						for (final PDM.DataInstr instr : dataSegment) {
							System.out.printf("%8d [%s] %s\n", addr, (instr instanceof PDM.SIZE) ? " " : instr.size(),
									(instr instanceof PDM.LABEL ? "" : "  ") + instr.toString());
							addr += instr.size();
						}
					}
					System.out.println();
				}
			}

			// Upajmo, da kdaj pridemo to te tocke.
			// A zavedajmo se sledecega:
			// 1. Prevod je zaradi napak v programu lahko napacen :-o
			// 2. Izvorni program se zdalec ni tisto, kar je programer hotel, da bi bil ;-)
			Report.info("Done.");
		} catch (Report.Error error) {
			// Izpis opisa napake.
			System.err.println(error.getMessage());
			System.exit(1);
		}
	}

}
