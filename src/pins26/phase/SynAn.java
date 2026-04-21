package pins26.phase;

import java.util.*;

import pins26.common.*;

/**
 * Sintaksni analizator.
 */
public class SynAn implements AutoCloseable {

	/** Leksikalni analizator. */
	private final LexAn lexAn;

	/**
	 * Ustvari nov sintaksni analizator.
	 *
	 * @param srcFileName Ime izvorne datoteke.
	 */
	public SynAn(final String srcFileName) {
		this.lexAn = new LexAn(srcFileName);
	}

    private HashMap<AST.Node, Report.Locatable> attrLoc;
	public boolean trace = false;

	@Override
	public void close() {
		lexAn.close();
	}

	/**
	 * Prevzame leksikalni analizator od leksikalnega analizatorja in preveri, ali
	 * je prave vrste.
	 *
	 * @param symbol Pricakovana vrsta leksikalnega simbola.
	 * @return Prevzeti leksikalni simbol.
	 */
	private Token check(Token.Symbol symbol) {
		final Token token = lexAn.takeToken();
		if (token.symbol() != symbol)
			throw new Report.Error(token, "Unexpected symbol '" + token.lexeme() + "'.");
		return token;
	}

	/**
	 * Opravi sintaksno analizo.
	 */
    public AST.Node parse(HashMap<AST.Node, Report.Locatable> attrLoc) {
        this.attrLoc = attrLoc;
        final AST.Nodes<AST.MainDef> defs = parseProgram();
        if (lexAn.peekToken().symbol() != Token.Symbol.EOF)
            Report.warning(lexAn.peekToken(),
                    "Unexpected text '" + lexAn.peekToken().lexeme() + "...' at the end of the program.");
        return defs;
    }

	/**
	 * POSTFIX_OP: PTR
	 * PREFIX_OP: NOT, ADD, SUB, PTR
	 * MULT_OP: MUL, DIV, MOD
	 * ADD_OP: ADD, SUB
	 * COMPARE_OP: EQU, NEQ, GTH, LTH, GEQ, LEQ
	 *
	 * LL(1)
	 * */
	private int indent = 0;

	private AST.Nodes<AST.MainDef> parseProgram() {
		if (trace) System.out.println("program ->");
		List<AST.MainDef> defs = new ArrayList<>();
		Token t = lexAn.peekToken();
		switch (t.symbol()) {
			case FUN, VAR:
				defs.add(definition());
				defs.addAll(defOpt());
				break;
			case EOF:
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		return new AST.Nodes<>(defs);
	}
	private List<AST.MainDef> defOpt() {
		indent++;
		if (trace) System.out.println(" ".repeat(indent) + "defOpt ->");
		List<AST.MainDef> definitions = new ArrayList<>();
		Token t = lexAn.peekToken();
		switch (t.symbol()) {
			case FUN, VAR:
				AST.MainDef nextDef = definition();
				definitions.add(nextDef);
				attrLoc.put(nextDef, t);
				definitions.addAll(defOpt());
				break;
			case IN, EOF:
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
		return definitions;
	}
	private AST.MainDef definition() {
		indent++;
		Token t = lexAn.peekToken();
		if (trace) System.out.println(" ".repeat(indent) + "definition ->" + t.toString() + " " + t.toString());
		switch (t.symbol()) {
			case FUN:
				Token funToken = check(Token.Symbol.FUN);
				Token funName = check(Token.Symbol.IDENTIFIER);
				check(Token.Symbol.LPAREN);
				List<AST.ParDef> funParams = parameters();
				check(Token.Symbol.RPAREN);
				List<AST.Stmt> funBody = fun_statements_opt();

				AST.FunDef funDef = new AST.FunDef(funName.lexeme(), funParams, funBody);
				attrLoc.put(funDef, funToken);
				return funDef;
			case VAR:
				Token varToken = check(Token.Symbol.VAR);
				Token varName = check(Token.Symbol.IDENTIFIER);
				check(Token.Symbol.ASSIGN);
				List<AST.Init> initializers = initializers();

				AST.VarDef varDef = new AST.VarDef(varName.lexeme(), initializers);
				attrLoc.put(varDef, varToken);
				return varDef;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
	}
	private List<AST.Stmt> fun_statements_opt() {
		indent++;
		Token t = lexAn.peekToken();
		if (trace) System.out.println(" ".repeat(indent) + "fun_statements_opt ->"+ t.toString());
		List<AST.Stmt> statements = new ArrayList<>();
		switch (t.symbol()) {
			case ASSIGN:
				check(Token.Symbol.ASSIGN);
				statements = statements();
				break;
			case FUN, VAR, IN, EOF:
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
		return statements;
	}
	private List<AST.ParDef> parameters() {
		indent++;
		Token t = lexAn.peekToken();
		if (trace) System.out.println(" ".repeat(indent) + "parameters ->" + t.toString());
		List<AST.ParDef> params = new ArrayList<>();
		switch (t.symbol()) {
			case IDENTIFIER:
				Token paramName = check(Token.Symbol.IDENTIFIER);
				AST.ParDef param = new AST.ParDef(paramName.lexeme());
				attrLoc.put(param, paramName);
				params.add(param);
				params.addAll(params_opt());
				break;
			case RPAREN:
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
		return params;
	}
	private List<AST.ParDef> params_opt() {
		indent++;
		Token t = lexAn.peekToken();
		if (trace) System.out.println(" ".repeat(indent) + "params_opt ->" + t.toString());
		List<AST.ParDef> params = new ArrayList<>();
		switch (t.symbol()) {
			case COMMA:
				check(Token.Symbol.COMMA);
				Token paramName = check(Token.Symbol.IDENTIFIER);
				AST.ParDef param = new AST.ParDef(paramName.lexeme());
				attrLoc.put(param, paramName);
				params.add(param);
				params.addAll(params_opt());
				params_opt();
				break;
			case RPAREN:
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
		return params;
	}
	private List<AST.Stmt> statements() {
		indent++;
		Token t = lexAn.peekToken();
		if (trace) System.out.println(" ".repeat(indent) + "statements ->" + t.toString());
		List<AST.Stmt> stmts = new ArrayList<>();
		switch (t.symbol()) {
			case IDENTIFIER, LPAREN, WHILE, LET, IF, ADD, SUB, NOT, PTR, INTCONST, CHARCONST, STRINGCONST:
				AST.Stmt statement = statement();
				stmts.add(statement);
				attrLoc.put(statement, t);
				check(Token.Symbol.SEMIC);
				stmts.addAll(statements_opt());
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
		return stmts;
	}
	private List<AST.Stmt> statements_opt() {
		indent++;
		Token t = lexAn.peekToken();
		if (trace) System.out.println(" ".repeat(indent) + "statements_opt ->" + t.toString());
		List<AST.Stmt> stmts = new ArrayList<>();
		switch (t.symbol()) {
			case FUN, VAR, END, IN, ELSE, EOF:
				break;
			case IDENTIFIER, LPAREN, WHILE, LET, IF, ADD, SUB, NOT, PTR, INTCONST, CHARCONST, STRINGCONST:
				AST.Stmt statement = statement();
				stmts.add(statement);
				attrLoc.put(statement, t);
				check(Token.Symbol.SEMIC);
				stmts.addAll(statements_opt());
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
		return stmts;
	}
	private AST.Stmt statement() {
		indent++;
		Token t = lexAn.peekToken();
		if (trace) System.out.println(" ".repeat(indent) + "statement ->" + t.toString());
		switch (t.symbol()) {
			case WHILE:
				Token whileToken = check(Token.Symbol.WHILE);
				AST.Expr cond = expression();
				check(Token.Symbol.DO);
				List<AST.Stmt> whileBody = statements();
				check(Token.Symbol.END);
				AST.WhileStmt whileStmt = new AST.WhileStmt(cond, whileBody);
				attrLoc.put(whileStmt, whileToken);
				indent--;
				return whileStmt;
			case LET:
				Token letToken = check(Token.Symbol.LET);
				List<AST.MainDef> letDefs = new ArrayList<>();
				letDefs.add(definition());
				letDefs.addAll(defOpt());
				check(Token.Symbol.IN);
				List<AST.Stmt> letBody = statements();
				check(Token.Symbol.END);
				AST.LetStmt letStmt = new AST.LetStmt(letDefs, letBody);
				attrLoc.put(letStmt, letToken);
				indent--;
				return letStmt;
			case IF:
				Token ifToken = check(Token.Symbol.IF);
				AST.Expr ifCond = expression();
				check(Token.Symbol.THEN);
				List<AST.Stmt> thenStmts = statements();
				List<AST.Stmt> elseStmts = else_opt();
				check(Token.Symbol.END);
				AST.IfStmt ifStmt = new AST.IfStmt(ifCond, thenStmts, elseStmts);
				attrLoc.put(ifStmt, ifToken);
				indent--;
				return ifStmt;
			case IDENTIFIER, LPAREN, ADD, SUB, NOT, PTR, INTCONST, CHARCONST, STRINGCONST:
				AST.Expr expr = expression();
				indent--;
				return expr_assign_opt(expr);
			default:
				throw new Report.Error("Unexpected token: " + t);
		}

	}
	private AST.Stmt expr_assign_opt(AST.Expr expr) {
		indent++;
		Token t = lexAn.peekToken();
		if (trace) System.out.println(" ".repeat(indent) + "expr_assign_opt ->" + t.toString());
		switch (t.symbol()) {
			case SEMIC:
				AST.ExprStmt exprStmt = new AST.ExprStmt(expr);
				attrLoc.put(exprStmt, lexAn.peekToken());
				indent--;
				return exprStmt;
			case ASSIGN:
				Token assignToken = check(Token.Symbol.ASSIGN);
				AST.Expr value = expression();
				AST.AssignStmt assignStmt = new AST.AssignStmt(expr, value);
				attrLoc.put(assignStmt, assignToken);
				indent--;
				return assignStmt;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}

	}
	private List<AST.Stmt> else_opt() {
		indent++;
		Token t = lexAn.peekToken();
		if (trace) System.out.println(" ".repeat(indent) + "else_opt ->" + t.toString());
		List<AST.Stmt> elseStmts = new ArrayList<>();
		switch (t.symbol()) {
			case END:
				break;
			case ELSE:
				check(Token.Symbol.ELSE);
				elseStmts = statements();
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
		return elseStmts;
	}
	private AST.Expr expression() {
		indent++;
		Token t = lexAn.peekToken();
		if (trace) System.out.println(" ".repeat(indent) + "expression ->" + t.toString());
		switch (t.symbol()) {
			case IDENTIFIER, LPAREN, ADD, SUB, NOT, PTR, INTCONST, CHARCONST, STRINGCONST:
				AST.Expr expr = disjunction_expr();
				attrLoc.put(expr, t);
				indent--;
				return expr;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
	}
	private AST.Expr disjunction_expr() {
		indent++;
		Token t = lexAn.peekToken();
		if (trace) System.out.println(" ".repeat(indent) + "disjunction_expr ->" + t.toString());
		switch (t.symbol()) {
			case IDENTIFIER, LPAREN, ADD, SUB, NOT, PTR, INTCONST, CHARCONST, STRINGCONST:
				AST.Expr left = conjunction_expr();
				return disjunction_opt(left);
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
	}
	private AST.Expr disjunction_opt(AST.Expr left) {
		indent++;
		Token t = lexAn.peekToken();
		if (trace) System.out.println(" ".repeat(indent) + "disjunction_opt ->" + t.toString());
		switch (t.symbol()) {
			case RPAREN, ASSIGN, COMMA, SEMIC, DO, THEN:
				return left;
			case OR:
				Token orToken = check(Token.Symbol.OR);
				AST.Expr right = conjunction_expr();
				AST.BinExpr binExpr = new AST.BinExpr(AST.BinExpr.Oper.OR, left, right);
				attrLoc.put(binExpr, orToken);
				return disjunction_opt(binExpr);
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
	}
	private AST.Expr conjunction_expr() {
		indent++;
		Token t = lexAn.peekToken();
		if (trace) System.out.println(" ".repeat(indent) + "conjunction_expr ->" + t.toString());
		switch (t.symbol()) {
			case IDENTIFIER, LPAREN, ADD, SUB, NOT, PTR, INTCONST, CHARCONST, STRINGCONST:
				AST.Expr left = compare_expr();
				return conjunction_opt(left);
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
	}
	private AST.Expr conjunction_opt(AST.Expr left) {
		indent++;
		Token t = lexAn.peekToken();
		if (trace) System.out.println(" ".repeat(indent) + "conjunction_opt ->" + t.toString());
		switch (t.symbol()) {
			case RPAREN, ASSIGN, COMMA, SEMIC, DO, THEN:
				return left;
			case OR:
				return left;
			case AND:
				Token andToken = check(Token.Symbol.AND);
				AST.Expr right = compare_expr();
				AST.BinExpr binExpr = new AST.BinExpr(AST.BinExpr.Oper.AND, left, right);
				attrLoc.put(binExpr, andToken);
				return conjunction_opt(binExpr);
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
	}
	private AST.Expr compare_expr() {
		indent++;
		Token t = lexAn.peekToken();
		if (trace) System.out.println(" ".repeat(indent) + "compare_expr ->" + t.toString());
		switch (t.symbol()) {
			case IDENTIFIER, LPAREN, ADD, SUB, NOT, PTR, INTCONST, CHARCONST, STRINGCONST:
				AST.Expr left = add_expr();
				return compare_opt(left);
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
	}
	private AST.Expr compare_opt(AST.Expr left) {
		indent++;
		Token t = lexAn.peekToken();
		if (trace) System.out.println(" ".repeat(indent) + "compare_opt ->" + t.toString());
		switch (t.symbol()) {
			case RPAREN, ASSIGN, COMMA, SEMIC, DO, THEN:
				return left;
			case OR, AND:
				return left;
			case EQU, NEQ, LTH, GTH, LEQ, GEQ:
				Token compOpToken = comp_operator();
				AST.Expr right = add_expr();
				AST.BinExpr binExpr = new AST.BinExpr(mapBinOper(compOpToken.symbol()), left, right);
				attrLoc.put(binExpr, compOpToken);
				return compare_opt(binExpr);
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
	}
	// COMPARISON OPERATOR SET
	private Token comp_operator() {
		indent++;
		Token t = lexAn.peekToken();
		if (trace) System.out.println(" ".repeat(indent) + "operator: " + t.toString());
        return switch (t.symbol()) {
            case EQU -> check(Token.Symbol.EQU);
            case NEQ -> check(Token.Symbol.NEQ);
            case LTH -> check(Token.Symbol.LTH);
            case GTH -> check(Token.Symbol.GTH);
            case LEQ -> check(Token.Symbol.LEQ);
            case GEQ -> check(Token.Symbol.GEQ);
            default -> throw new Report.Error("Unexpected token: " + t);
        };
	}

	private AST.Expr add_expr() {
		indent++;
		Token t = lexAn.peekToken();
		if (trace) System.out.println(" ".repeat(indent) + "add_expr ->" + t.toString());
		switch (t.symbol()) {
			case IDENTIFIER, LPAREN, ADD, SUB, NOT, PTR, INTCONST, CHARCONST, STRINGCONST:
				AST.Expr left = mul_expr();
				return add_opt(left);
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
	}
	private AST.Expr add_opt(AST.Expr left) {
		indent++;
		Token t = lexAn.peekToken();
		if (trace) System.out.println(" ".repeat(indent) + "add_opt ->" + t.toString());
		switch (t.symbol()) {
			case RPAREN, ASSIGN, COMMA, SEMIC, DO, THEN:
				return left;
			case OR, AND, EQU, NEQ, LTH, GTH, LEQ, GEQ:
				return left;
			case ADD, SUB:
				Token addOpToken = add_operator();
				AST.Expr right = mul_expr();
				AST.BinExpr binExpr = new AST.BinExpr(mapBinOper(addOpToken.symbol()), left, right);
				attrLoc.put(binExpr, addOpToken);
				return add_opt(binExpr);
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
	}
	// ADDITION OPERATOR SET
	private Token add_operator() {
		indent++;
		Token t = lexAn.peekToken();
		if (trace) System.out.println(" ".repeat(indent) + "operator: " + t.toString());
        return switch (t.symbol()) {
            case ADD -> check(Token.Symbol.ADD);
            case SUB -> check(Token.Symbol.SUB);
            default -> throw new Report.Error("Unexpected token: " + t);
        };
	}

	private AST.Expr mul_expr() {
		indent++;
		Token t = lexAn.peekToken();
		if (trace) System.out.println(" ".repeat(indent) + "mul_expr ->" + t.toString());
		switch (t.symbol()) {
			case IDENTIFIER, LPAREN, ADD, SUB, NOT, PTR, INTCONST, CHARCONST, STRINGCONST:
				AST.Expr left = prefix_expr();
				return mul_opt(left);
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
	}
	private AST.Expr mul_opt(AST.Expr left) {
		indent++;
		Token t = lexAn.peekToken();
		if (trace) System.out.println(" ".repeat(indent) + "mul_opt ->" + t.toString());
		switch (t.symbol()) {
			case RPAREN, ASSIGN, COMMA, SEMIC, DO, THEN:
				return left;
			case OR, AND, EQU, NEQ, LTH, GTH, LEQ, GEQ, ADD, SUB:
				return left;
			case MUL, DIV, MOD:
				Token mulOpToken = mul_operator();
				AST.Expr right = prefix_expr();
				AST.BinExpr binExpr = new AST.BinExpr(mapBinOper(mulOpToken.symbol()), left, right);
				attrLoc.put(binExpr, mulOpToken);
				return mul_opt(binExpr);
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
	}
	// MULTIPLICATION OPERATOR SET
	private Token mul_operator() {
		indent++;
		Token t = lexAn.peekToken();
		if (trace) System.out.println(" ".repeat(indent) + "operator: " + t.toString());
        return switch (t.symbol()) {
            case MUL -> check(Token.Symbol.MUL);
            case DIV -> check(Token.Symbol.DIV);
            case MOD -> check(Token.Symbol.MOD);
            default -> throw new Report.Error("Unexpected token: " + t);
        };
	}

	private AST.Expr prefix_expr() {
		indent++;
		Token t = lexAn.peekToken();
		if (trace) System.out.println(" ".repeat(indent) + "prefix_expr ->" + t.toString());
		switch (t.symbol()) {
			case IDENTIFIER, LPAREN, INTCONST, CHARCONST, STRINGCONST:
				return postfix_expr();
			case ADD, SUB, NOT:
				Token prefixOpToken = prefix_operator();
				AST.Expr operand = prefix_expr();
				AST.UnExpr unExpr = new AST.UnExpr(mapUnOper(prefixOpToken.symbol()), operand);
				attrLoc.put(unExpr, prefixOpToken);
				return unExpr;
			case PTR:
				Token prefixPtrToken = prefix_operator();
				AST.Expr prefixPtrOperand = prefix_expr();
				AST.UnExpr prefixPtrExpr = new AST.UnExpr(AST.UnExpr.Oper.MEMADDR, prefixPtrOperand);
				attrLoc.put(prefixPtrExpr, prefixPtrToken);
				return prefixPtrExpr;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
	}
	// PREFIX OPERATOR SET
	private Token prefix_operator() {
		indent++;
		Token t = lexAn.peekToken();
		if (trace) System.out.println(" ".repeat(indent) + "operator: " + t.toString());
        return switch (t.symbol()) {
            case ADD -> check(Token.Symbol.ADD);
            case SUB -> check(Token.Symbol.SUB);
            case NOT -> check(Token.Symbol.NOT);
            case PTR -> check(Token.Symbol.PTR);
            default -> throw new Report.Error("Unexpected token: " + t);
        };
	}
	private AST.Expr postfix_expr() {
		indent++;
		Token t = lexAn.peekToken();
		if (trace) System.out.println(" ".repeat(indent) + "postfix_expr ->" + t.toString());
		switch (t.symbol()) {
			case IDENTIFIER, LPAREN, INTCONST, CHARCONST, STRINGCONST:
				AST.Expr expr = atom_expr();
				return postfix_opt(expr);
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
	}
	private AST.Expr postfix_opt(AST.Expr left) {
		indent++;
		Token t = lexAn.peekToken();
		if (trace) System.out.println(" ".repeat(indent) + "postfix_opt ->" + t.toString());
		switch (t.symbol()) {
			case RPAREN, ASSIGN, COMMA, SEMIC, DO, THEN, OR, AND,
					EQU, NEQ, LTH, GTH, LEQ, GEQ, ADD, SUB, MUL, DIV, MOD:
				return left;
			case PTR:
				Token ptrToken = postfix_operator();
				AST.UnExpr unExpr = new AST.UnExpr(AST.UnExpr.Oper.VALUEAT, left);
				attrLoc.put(unExpr, ptrToken);
				return postfix_opt(unExpr);
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
	}
	// POSTFIX OPERATOR SET
	private Token postfix_operator() {
		indent++;
		Token t = lexAn.peekToken();
		if (trace) System.out.println(" ".repeat(indent) + "operator: " + t.toString());
		switch (t.symbol()) {
			case PTR:
				return check(Token.Symbol.PTR);
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
	}
	private AST.Expr atom_expr() {
		indent++;
		Token t = lexAn.peekToken();
		if (trace) System.out.println(" ".repeat(indent) + "atom_expr ->" + t.toString());
		switch (t.symbol()) {
			case IDENTIFIER:
				Token identToken = check(Token.Symbol.IDENTIFIER);
				AST.Expr identifierExpr = expr_args_opt(identToken);
				attrLoc.put(identifierExpr, identToken);
				return identifierExpr;
			case LPAREN:
				Token lparenToken = check(Token.Symbol.LPAREN);
				AST.Expr parenExpr = expression();
				Token rparenToken = check(Token.Symbol.RPAREN);

				Report.Location updatedLocation = new Report.Location(
						lparenToken.location().begLine(),
						lparenToken.location().begColumn(),
						rparenToken.location().endLine(),
						rparenToken.location().endColumn()
				);
				attrLoc.put(parenExpr, updatedLocation);
				return parenExpr;
			case INTCONST:
				Token intToken = check(Token.Symbol.INTCONST);
				AST.AtomExpr intExpr = new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, intToken.lexeme());
				attrLoc.put(intExpr, intToken);
				return intExpr;
			case CHARCONST:
				Token charToken = check(Token.Symbol.CHARCONST);
				AST.AtomExpr charExpr = new AST.AtomExpr(AST.AtomExpr.Type.CHRCONST, charToken.lexeme());
				attrLoc.put(charExpr, charToken);
				return charExpr;
			case STRINGCONST:
				Token stringToken = check(Token.Symbol.STRINGCONST);
				AST.AtomExpr stringExpr = new AST.AtomExpr(AST.AtomExpr.Type.STRCONST, stringToken.lexeme());
				attrLoc.put(stringExpr, stringToken);
				return stringExpr;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
	}
	private AST.Expr expr_args_opt(Token identToken) {
		indent++;
		Token t = lexAn.peekToken();
		if (trace) System.out.println(" ".repeat(indent) + "expr_args_opt ->" + t.toString());
		switch (t.symbol()) {
			case LPAREN:
				Token lparenToken = check(Token.Symbol.LPAREN);
				List<AST.Expr> args = arguments();
				Token rparenToken = check(Token.Symbol.RPAREN);
				AST.CallExpr callExpr = new AST.CallExpr(identToken.lexeme(), args);
				attrLoc.put(callExpr, lparenToken);
				return callExpr;
			case RPAREN, ASSIGN, COMMA, SEMIC, DO, THEN, OR, AND,
					EQU, NEQ, LTH, GTH, LEQ, GEQ, ADD, SUB, MUL, DIV, MOD, PTR:
				AST.VarExpr varExpr = new AST.VarExpr(identToken.lexeme());
				attrLoc.put(varExpr, identToken);
				return varExpr;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
	}
	private List<AST.Expr> arguments() {
		indent++;
		Token t = lexAn.peekToken();
		if (trace) System.out.println(" ".repeat(indent) + "arguments ->" + t.toString());
		List<AST.Expr> args = new ArrayList<>();
		switch (t.symbol()) {
			case IDENTIFIER, LPAREN, ADD, SUB, NOT, PTR, INTCONST, CHARCONST, STRINGCONST:
				args.add(expression());
				args_opt(args);
				break;
			case RPAREN:
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		return args;
	}
	private void args_opt(List<AST.Expr> args) {
		indent++;
		Token t = lexAn.peekToken();
		if (trace) System.out.println(" ".repeat(indent) + "args_opt ->" + t.toString());
		switch (t.symbol()) {
			case COMMA:
				check(Token.Symbol.COMMA);
				args.add(expression());
				args_opt(args);
				break;
			case RPAREN:
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
	}
	private List<AST.Init> initializers() {
		indent++;
		Token t = lexAn.peekToken();
		if (trace) System.out.println(" ".repeat(indent) + "initializers ->" + t.toString());
		List<AST.Init> inits = new ArrayList<>();
		switch (t.symbol()) {
			case FUN, VAR, IN, EOF:
				break;
			case INTCONST, CHARCONST, STRINGCONST:
				inits.add(initializer());
				init_opt(inits);
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		return inits;
	}
	private void init_opt(List<AST.Init> inits) {
		indent++;
		Token t = lexAn.peekToken();
		if (trace) System.out.println(" ".repeat(indent) + "init_opt ->" + t.toString());
		switch (t.symbol()) {
			case FUN, VAR, IN, EOF:
				break;
			case COMMA:
				check(Token.Symbol.COMMA);
				inits.add(initializer());
				init_opt(inits);
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
	}
	private AST.Init initializer() {
		indent++;
		Token t = lexAn.peekToken();
		if (trace) System.out.println(" ".repeat(indent) + "initializer ->" + t.toString());
		switch (t.symbol()) {
			case INTCONST:
				Token intToken = check(Token.Symbol.INTCONST);
				AST.AtomExpr optValue = opt_mul();
				if (optValue != null) {
					// INTCONST * constant
					AST.AtomExpr num = new AST.AtomExpr(
							AST.AtomExpr.Type.INTCONST,
							intToken.lexeme()
					);
					attrLoc.put(num, intToken);

					AST.Init init = new AST.Init(num, optValue);
					attrLoc.put(init, intToken);
					indent--;
					return init;
				} else {
					// just INTCONST → value, num = 1
					AST.AtomExpr value = new AST.AtomExpr(
							AST.AtomExpr.Type.INTCONST,
							intToken.lexeme()
					);
					attrLoc.put(value, intToken);

					AST.AtomExpr num = new AST.AtomExpr(
							AST.AtomExpr.Type.INTCONST,
							"1"
					);
					attrLoc.put(num, intToken);

					AST.Init init = new AST.Init(num, value);
					attrLoc.put(init, intToken);
					indent--;
					return init;
				}
			case CHARCONST, STRINGCONST:
				AST.AtomExpr value = const_non_int();

				AST.AtomExpr num = new AST.AtomExpr(
						AST.AtomExpr.Type.INTCONST,
						"1"
				);
				attrLoc.put(num, attrLoc.get(value));

				AST.Init init = new AST.Init(num, value);
				attrLoc.put(init, attrLoc.get(value));
				indent--;
				return init;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
	}

	private AST.AtomExpr opt_mul() {
		indent++;
		Token t = lexAn.peekToken();
		if (trace) System.out.println(" ".repeat(indent) + "initializer ->" + t.toString());
		switch (t.symbol()) {
			case MUL:
				check(Token.Symbol.MUL);
				AST.AtomExpr value = constant();
				return value;
			case FUN, VAR, COMMA, IN, EOF:
				return null;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
	}
	private AST.AtomExpr constant() {
		indent++;
		Token t = lexAn.peekToken();
		if (trace) System.out.println(" ".repeat(indent) + "constant: " + t.symbol() + " " + t.toString());
		switch (t.symbol()) {
			case INTCONST:
				Token intToken = check(Token.Symbol.INTCONST);
				AST.AtomExpr intExpr = new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, intToken.lexeme());
				attrLoc.put(intExpr, intToken);
				return intExpr;
			case CHARCONST:
				Token charToken = check(Token.Symbol.CHARCONST);
				AST.AtomExpr charExpr = new AST.AtomExpr(AST.AtomExpr.Type.CHRCONST, charToken.lexeme());
				attrLoc.put(charExpr, charToken);
				return charExpr;
			case STRINGCONST:
				Token stringToken = check(Token.Symbol.STRINGCONST);
				AST.AtomExpr stringExpr = new AST.AtomExpr(AST.AtomExpr.Type.STRCONST, stringToken.lexeme());
				attrLoc.put(stringExpr, stringToken);
				return stringExpr;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
	}
	private AST.AtomExpr const_non_int() {
		indent++;
		Token t = lexAn.peekToken();
		if (trace) System.out.println(" ".repeat(indent) + "cons_not_int: " + t.symbol() + " " + t.toString());
		switch (t.symbol()) {
			case CHARCONST:
				Token charToken = check(Token.Symbol.CHARCONST);
				AST.AtomExpr charExpr = new AST.AtomExpr(AST.AtomExpr.Type.CHRCONST, charToken.lexeme());
				attrLoc.put(charExpr, charToken);
				return charExpr;
			case STRINGCONST:
				Token stringToken = check(Token.Symbol.STRINGCONST);
				AST.AtomExpr stringExpr = new AST.AtomExpr(AST.AtomExpr.Type.STRCONST, stringToken.lexeme());
				attrLoc.put(stringExpr, stringToken);
				return stringExpr;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
	}

	private AST.BinExpr.Oper mapBinOper(Token.Symbol sym) {
		return switch (sym) {
			case OR  -> AST.BinExpr.Oper.OR;
			case AND -> AST.BinExpr.Oper.AND;

			case EQU -> AST.BinExpr.Oper.EQU;
			case NEQ -> AST.BinExpr.Oper.NEQ;
			case LTH -> AST.BinExpr.Oper.LTH;
			case GTH -> AST.BinExpr.Oper.GTH;
			case LEQ -> AST.BinExpr.Oper.LEQ;
			case GEQ -> AST.BinExpr.Oper.GEQ;

			case ADD -> AST.BinExpr.Oper.ADD;
			case SUB -> AST.BinExpr.Oper.SUB;

			case MUL -> AST.BinExpr.Oper.MUL;
			case DIV -> AST.BinExpr.Oper.DIV;
			case MOD -> AST.BinExpr.Oper.MOD;

			default -> throw new Report.Error("Not a binary operator: " + sym);
		};
	}
	private AST.UnExpr.Oper mapUnOper(Token.Symbol sym) {
		return switch (sym) {
			case ADD  -> AST.UnExpr.Oper.ADD;
			case SUB  -> AST.UnExpr.Oper.SUB;
			case NOT  -> AST.UnExpr.Oper.NOT;

			default -> throw new Report.Error("Not a binary operator: " + sym);
		};
	}


	// --- ZAGON ---

	/**
	 * Zagon sintaksnega analizatorja kot samostojnega programa.
	 *
	 * @param cmdLineArgs Argumenti v ukazni vrstici.
	 */
	public static void main(final String[] cmdLineArgs) {
		System.out.println("This is PINS'26 compiler (syntax analysis):");

		try {
			if (cmdLineArgs.length == 0)
				throw new Report.Error("No source file specified in the command line.");
			if (cmdLineArgs.length > 1)
				Report.warning("Unused arguments in the command line.");

			try (SynAn synAn = new SynAn(cmdLineArgs[0])) {
				synAn.trace = true;
				synAn.parse(synAn.attrLoc);
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
