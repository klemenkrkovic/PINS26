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
		System.out.println("program ->");
		Token t = lexAn.peekToken();
		switch (t.symbol()) {
			case FUN, VAR:
				definition();
				defOpt();
				break;
			case EOF:
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
        return null; // TODO implement changes
	}
	private void defOpt() {
		indent++;
		System.out.println(" ".repeat(indent) + "defOpt ->");
		Token t = lexAn.peekToken();
		switch (t.symbol()) {
			case FUN, VAR:
				definition();
				defOpt();
				break;
			case IN, EOF:
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
	}
	private void definition() {
		indent++;
		Token t = lexAn.peekToken();
		System.out.println(" ".repeat(indent) + "definition ->" + t.toString() + " " + t.toString());
		switch (t.symbol()) {
			case FUN:
				check(Token.Symbol.FUN);
				check(Token.Symbol.IDENTIFIER);
				check(Token.Symbol.LPAREN);
				parameters();
				check(Token.Symbol.RPAREN);
				fun_statements_opt();
				break;
			case VAR:
				check(Token.Symbol.VAR);
				check(Token.Symbol.IDENTIFIER);
				check(Token.Symbol.ASSIGN);
				initializers();
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
	}
	private void fun_statements_opt() {
		indent++;
		Token t = lexAn.peekToken();
		System.out.println(" ".repeat(indent) + "fun_statements_opt ->"+ t.toString());
		switch (t.symbol()) {
			case ASSIGN:
				check(Token.Symbol.ASSIGN);
				statements();
				break;
			case FUN, VAR, IN, EOF:
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
	}
	private void parameters() {
		indent++;
		Token t = lexAn.peekToken();
		System.out.println(" ".repeat(indent) + "parameters ->" + t.toString());
		switch (t.symbol()) {
			case IDENTIFIER:
				check(Token.Symbol.IDENTIFIER);
				params_opt();
				break;
			case RPAREN:
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
	}
	private void params_opt() {
		indent++;
		Token t = lexAn.peekToken();
		System.out.println(" ".repeat(indent) + "params_opt ->" + t.toString());
		switch (t.symbol()) {
			case COMMA:
				check(Token.Symbol.COMMA);
				check(Token.Symbol.IDENTIFIER);
				params_opt();
				break;
			case RPAREN:
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
	}
	private void statements() {
		indent++;
		Token t = lexAn.peekToken();
		System.out.println(" ".repeat(indent) + "statements ->" + t.toString());
		switch (t.symbol()) {
			case IDENTIFIER, LPAREN, WHILE, LET, IF, ADD, SUB, NOT, PTR, INTCONST, CHARCONST, STRINGCONST:
				statement();
				check(Token.Symbol.SEMIC);
				statements_opt();
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
	}
	private void statements_opt() {
		indent++;
		Token t = lexAn.peekToken();
		System.out.println(" ".repeat(indent) + "statements_opt ->" + t.toString());
		switch (t.symbol()) {
			case FUN, VAR, END, IN, ELSE, EOF:
				break;
			case IDENTIFIER, LPAREN, WHILE, LET, IF, ADD, SUB, NOT, PTR, INTCONST, CHARCONST, STRINGCONST:
				statement();
				check(Token.Symbol.SEMIC);
				statements_opt();
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
	}
	private void statement() {
		indent++;
		Token t = lexAn.peekToken();
		System.out.println(" ".repeat(indent) + "statement ->" + t.toString());
		switch (t.symbol()) {
			case WHILE:
				check(Token.Symbol.WHILE);
				expression();
				check(Token.Symbol.DO);
				statements();
				check(Token.Symbol.END);
				break;
			case LET:
				check(Token.Symbol.LET);
				definition();
				defOpt();
				check(Token.Symbol.IN);
				statements();
				check(Token.Symbol.END);
				break;
			case IF:
				check(Token.Symbol.IF);
				expression();
				check(Token.Symbol.THEN);
				statements();
				else_opt();
				check(Token.Symbol.END);
				break;
			case IDENTIFIER, LPAREN, ADD, SUB, NOT, PTR, INTCONST, CHARCONST, STRINGCONST:
				expression();
				expr_assign_opt();
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
	}
	private void expr_assign_opt() {
		indent++;
		Token t = lexAn.peekToken();
		System.out.println(" ".repeat(indent) + "expr_assign_opt ->" + t.toString());
		switch (t.symbol()) {
			case SEMIC:
				break;
			case ASSIGN:
				check(Token.Symbol.ASSIGN);
				expression();
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
	}
	private void else_opt() {
		indent++;
		Token t = lexAn.peekToken();
		System.out.println(" ".repeat(indent) + "else_opt ->" + t.toString());
		switch (t.symbol()) {
			case END:
				break;
			case ELSE:
				check(Token.Symbol.ELSE);
				statements();
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
	}
	private void expression() {
		indent++;
		Token t = lexAn.peekToken();
		System.out.println(" ".repeat(indent) + "expression ->" + t.toString());
		switch (t.symbol()) {
			case IDENTIFIER, LPAREN, ADD, SUB, NOT, PTR, INTCONST, CHARCONST, STRINGCONST:
				disjunction_expr();
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
	}
	private void disjunction_expr() {
		indent++;
		Token t = lexAn.peekToken();
		System.out.println(" ".repeat(indent) + "disjunction_expr ->" + t.toString());
		switch (t.symbol()) {
			case IDENTIFIER, LPAREN, ADD, SUB, NOT, PTR, INTCONST, CHARCONST, STRINGCONST:
				conjunction_expr();
				disjunction_opt();
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
	}
	private void disjunction_opt() {
		indent++;
		Token t = lexAn.peekToken();
		System.out.println(" ".repeat(indent) + "disjunction_opt ->" + t.toString());
		switch (t.symbol()) {
			case RPAREN, ASSIGN, COMMA, SEMIC, DO, THEN:
				break;
			case OR:
				check(Token.Symbol.OR);
				conjunction_expr();
				disjunction_opt();
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
	}
	private void conjunction_expr() {
		indent++;
		Token t = lexAn.peekToken();
		System.out.println(" ".repeat(indent) + "conjunction_expr ->" + t.toString());
		switch (t.symbol()) {
			case IDENTIFIER, LPAREN, ADD, SUB, NOT, PTR, INTCONST, CHARCONST, STRINGCONST:
				compare_expr();
				conjunction_opt();
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
	}
	private void conjunction_opt() {
		indent++;
		Token t = lexAn.peekToken();
		System.out.println(" ".repeat(indent) + "conjunction_opt ->" + t.toString());
		switch (t.symbol()) {
			case RPAREN, ASSIGN, COMMA, SEMIC, DO, THEN:
				break;
			case OR:
				break;
			case AND:
				check(Token.Symbol.AND);
				compare_expr();
				conjunction_opt();
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
	}
	private void compare_expr() {
		indent++;
		Token t = lexAn.peekToken();
		System.out.println(" ".repeat(indent) + "compare_expr ->" + t.toString());
		switch (t.symbol()) {
			case IDENTIFIER, LPAREN, ADD, SUB, NOT, PTR, INTCONST, CHARCONST, STRINGCONST:
				add_expr();
				compare_opt();
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
	}
	private void compare_opt() {
		indent++;
		Token t = lexAn.peekToken();
		System.out.println(" ".repeat(indent) + "compare_opt ->" + t.toString());
		switch (t.symbol()) {
			case RPAREN, ASSIGN, COMMA, SEMIC, DO, THEN:
				break;
			case OR, AND:
				break;
			case EQU, NEQ, LTH, GTH, LEQ, GEQ:
				comp_operator();
				add_expr();
				compare_opt();
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
	}
	// COMPARISON OPERATOR SET
	private void comp_operator() {
		indent++;
		Token t = lexAn.peekToken();
		System.out.println(" ".repeat(indent) + "operator: " + t.toString());
		switch (t.symbol()) {
			case EQU:
				check(Token.Symbol.EQU); break;
			case NEQ:
				check(Token.Symbol.NEQ); break;
			case LTH:
				check(Token.Symbol.LTH); break;
			case GTH:
				check(Token.Symbol.GTH); break;
			case LEQ:
				check(Token.Symbol.LEQ); break;
			case GEQ:
				check(Token.Symbol.GEQ); break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
	}

	private void add_expr() {
		indent++;
		Token t = lexAn.peekToken();
		System.out.println(" ".repeat(indent) + "add_expr ->" + t.toString());
		switch (t.symbol()) {
			case IDENTIFIER, LPAREN, ADD, SUB, NOT, PTR, INTCONST, CHARCONST, STRINGCONST:
				mul_expr();
				add_opt();
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
	}
	private void add_opt() {
		indent++;
		Token t = lexAn.peekToken();
		System.out.println(" ".repeat(indent) + "add_opt ->" + t.toString());
		switch (t.symbol()) {
			case RPAREN, ASSIGN, COMMA, SEMIC, DO, THEN:
				break;
			case OR, AND, EQU, NEQ, LTH, GTH, LEQ, GEQ:
				break;
			case ADD, SUB:
				add_operator();
				mul_expr();
				add_opt();
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
	}
	// ADDITION OPERATOR SET
	private void add_operator() {
		indent++;
		Token t = lexAn.peekToken();
		System.out.println(" ".repeat(indent) + "operator: " + t.toString());
		switch (t.symbol()) {
			case ADD:
				check(Token.Symbol.ADD); break;
			case SUB:
				check(Token.Symbol.SUB); break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
	}

	private void mul_expr() {
		indent++;
		Token t = lexAn.peekToken();
		System.out.println(" ".repeat(indent) + "mul_expr ->" + t.toString());
		switch (t.symbol()) {
			case IDENTIFIER, LPAREN, ADD, SUB, NOT, PTR, INTCONST, CHARCONST, STRINGCONST:
				prefix_expr();
				mul_opt();
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
	}

	private void mul_opt() {
		indent++;
		Token t = lexAn.peekToken();
		System.out.println(" ".repeat(indent) + "mul_opt ->" + t.toString());
		switch (t.symbol()) {
			case RPAREN, ASSIGN, COMMA, SEMIC, DO, THEN:
				break;
			case OR, AND, EQU, NEQ, LTH, GTH, LEQ, GEQ, ADD, SUB:
				break;
			case MUL, DIV, MOD:
				mul_operator();
				prefix_expr();
				mul_opt();
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
	}
	// MULTIPLICATION OPERATOR SET
	private void mul_operator() {
		indent++;
		Token t = lexAn.peekToken();
		System.out.println(" ".repeat(indent) + "operator: " + t.toString());
		switch (t.symbol()) {
			case MUL:
				check(Token.Symbol.MUL); break;
			case DIV:
				check(Token.Symbol.DIV); break;
			case MOD:
				check(Token.Symbol.MOD); break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
	}
	private void prefix_expr() {
		indent++;
		Token t = lexAn.peekToken();
		System.out.println(" ".repeat(indent) + "prefix_expr ->" + t.toString());
		switch (t.symbol()) {
			case IDENTIFIER, LPAREN, INTCONST, CHARCONST, STRINGCONST:
				postfix_expr();
				break;
			case ADD, SUB, NOT, PTR:
				prefix_operator();
				prefix_expr();
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
	}
	// PREFIX OPERATOR SET
	private void prefix_operator() {
		indent++;
		Token t = lexAn.peekToken();
		System.out.println(" ".repeat(indent) + "operator: " + t.toString());
		switch (t.symbol()) {
			case ADD:
				check(Token.Symbol.ADD); break;
			case SUB:
				check(Token.Symbol.SUB); break;
			case NOT:
				check(Token.Symbol.NOT); break;
			case PTR:
				check(Token.Symbol.PTR); break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
	}
	private void postfix_expr() {
		indent++;
		Token t = lexAn.peekToken();
		System.out.println(" ".repeat(indent) + "postfix_expr ->" + t.toString());
		switch (t.symbol()) {
			case IDENTIFIER, LPAREN, INTCONST, CHARCONST, STRINGCONST:
				atom_expr();
				postfix_opt();
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
	}
	private void postfix_opt() {
		indent++;
		Token t = lexAn.peekToken();
		System.out.println(" ".repeat(indent) + "postfix_opt ->" + t.toString());
		switch (t.symbol()) {
			case RPAREN, ASSIGN, COMMA, SEMIC, DO, THEN, OR, AND,
					EQU, NEQ, LTH, GTH, LEQ, GEQ, ADD, SUB, MUL, DIV, MOD:
				break;
			case PTR:
				postfix_operator();
				postfix_opt();
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
	}
	// POSTFIX OPERATOR SET
	private void postfix_operator() {
		indent++;
		Token t = lexAn.peekToken();
		System.out.println(" ".repeat(indent) + "operator: " + t.toString());
		switch (t.symbol()) {
			case PTR:
				check(Token.Symbol.PTR); break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
	}
	private void atom_expr() {
		indent++;
		Token t = lexAn.peekToken();
		System.out.println(" ".repeat(indent) + "atom_expr ->" + t.toString());
		switch (t.symbol()) {
			case IDENTIFIER:
				check(Token.Symbol.IDENTIFIER);
				expr_args_opt();
				break;
			case LPAREN:
				check(Token.Symbol.LPAREN);
				expression();
				check(Token.Symbol.RPAREN);
				break;
			case INTCONST:
				check(Token.Symbol.INTCONST);
				break;
			case CHARCONST:
				check(Token.Symbol.CHARCONST);
				break;
			case STRINGCONST:
				check(Token.Symbol.STRINGCONST);
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
	}
	private void expr_args_opt() {
		indent++;
		Token t = lexAn.peekToken();
		System.out.println(" ".repeat(indent) + "expr_args_opt ->" + t.toString());
		switch (t.symbol()) {
			case LPAREN:
				check(Token.Symbol.LPAREN);
				arguments();
				check(Token.Symbol.RPAREN);
				break;
			case RPAREN, ASSIGN, COMMA, SEMIC, DO, THEN, OR, AND,
					EQU, NEQ, LTH, GTH, LEQ, GEQ, ADD, SUB, MUL, DIV, MOD, PTR:
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
	}
	private void arguments() {
		indent++;
		Token t = lexAn.peekToken();
		System.out.println(" ".repeat(indent) + "arguments ->" + t.toString());
		switch (t.symbol()) {
			case IDENTIFIER, LPAREN, ADD, SUB, NOT, PTR, INTCONST, CHARCONST, STRINGCONST:
				expression();
				args_opt();
				break;
			case RPAREN:
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
	}
	private void args_opt() {
		indent++;
		Token t = lexAn.peekToken();
		System.out.println(" ".repeat(indent) + "args_opt ->" + t.toString());
		switch (t.symbol()) {
			case COMMA:
				check(Token.Symbol.COMMA);
				expression();
				args_opt();
				break;
			case RPAREN:
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
	}
	private void initializers() {
		indent++;
		Token t = lexAn.peekToken();
		System.out.println(" ".repeat(indent) + "initializers ->" + t.toString());
		switch (t.symbol()) {
			case FUN, VAR, IN, EOF:
				break;
			case INTCONST, CHARCONST, STRINGCONST:
				initializer();
				init_opt();
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
	}
	private void init_opt() {
		indent++;
		Token t = lexAn.peekToken();
		System.out.println(" ".repeat(indent) + "init_opt ->" + t.toString());
		switch (t.symbol()) {
			case FUN, VAR, IN, EOF:
				break;
			case COMMA:
				check(Token.Symbol.COMMA);
				initializer();
				init_opt();
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
	}
	private void initializer() {
		indent++;
		Token t = lexAn.peekToken();
		System.out.println(" ".repeat(indent) + "initializer ->" + t.toString());
		switch (t.symbol()) {
			case INTCONST:
				check(Token.Symbol.INTCONST);
				opt_mul();
				break;
			case CHARCONST, STRINGCONST:
				const_non_int();
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
	}

	private void opt_mul() {
		indent++;
		Token t = lexAn.peekToken();
		System.out.println(" ".repeat(indent) + "initializer ->" + t.toString());
		switch (t.symbol()) {
			case MUL:
				check(Token.Symbol.MUL);
				constant();
				break;
			case FUN, VAR, COMMA, IN, EOF:
				break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
	}
	private void constant() {
		indent++;
		Token t = lexAn.peekToken();
		System.out.println(" ".repeat(indent) + "constant: " + t.symbol() + " " + t.toString());
		switch (t.symbol()) {
			case INTCONST:
				check(Token.Symbol.INTCONST); break;
			case CHARCONST:
				check(Token.Symbol.CHARCONST); break;
			case STRINGCONST:
				check(Token.Symbol.STRINGCONST); break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
	}
	private void const_non_int() {
		indent++;
		Token t = lexAn.peekToken();
		System.out.println(" ".repeat(indent) + "cons_not_int: " + t.symbol() + " " + t.toString());
		switch (t.symbol()) {
			case CHARCONST:
				check(Token.Symbol.CHARCONST); break;
			case STRINGCONST:
				check(Token.Symbol.STRINGCONST); break;
			default:
				throw new Report.Error("Unexpected token: " + t);
		}
		indent--;
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
				synAn.parse();
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
