package pins26.phase;

import java.io.*;

import pins26.common.*;

/**
 * Leksikalni analizator.
 */
public class LexAn implements AutoCloseable {

	/** Izvorna datoteka. */
	private final Reader srcFile;

	/**
	 * Ustvari nov leksikalni analizator.
	 * 
	 * @param srcFileName Ime izvorne datoteke.
	 */
	public LexAn(final String srcFileName) {
		try {
			srcFile = new BufferedReader(new InputStreamReader(new FileInputStream(new File(srcFileName))));
			nextChar(); // Pripravi prvi znak izvorne datoteke (glej {@link nextChar}).
		} catch (FileNotFoundException __) {
			throw new Report.Error("Source file '" + srcFileName + "' not found.");
		}
	}

	@Override
	public void close() {
		try {
			srcFile.close();
		} catch (IOException __) {
			throw new Report.Error("Cannot close source file.");
		}
	}

	/** Trenutni znak izvorne datoteke (glej {@link nextChar}). */
	private int buffChar = -2;

	/** Vrstica trenutnega znaka izvorne datoteke (glej {@link nextChar}). */
	private int buffCharLine = 0;

	/** Stolpec trenutnega znaka izvorne datoteke (glej {@link nextChar}). */
	private int buffCharColumn = 0;

	/**
	 * Prebere naslednji znak izvorne datoteke.
	 * 
	 * Izvorno datoteko beremo znak po znak. Trenutni znak izvorne datoteke je
	 * shranjen v spremenljivki {@link buffChar}, vrstica in stolpec trenutnega
	 * znaka izvorne datoteke sta shranjena v spremenljivkah {@link buffCharLine} in
	 * {@link buffCharColumn}.
	 * 
	 * Zacetne vrednosti {@link buffChar}, {@link buffCharLine} in
	 * {@link buffCharColumn} so {@code '\n'}, {@code 0} in {@code 0}: branje prvega
	 * znaka izvorne datoteke bo na osnovi vrednosti {@code '\n'} spremenljivke
	 * {@link buffChar} prvemu znaku izvorne datoteke priredilo vrstico 1 in stolpec
	 * 1.
	 * 
	 * Pri branju izvorne datoteke se predpostavlja, da je v spremenljivki
	 * {@link buffChar} ves "cas veljaven znak. Zunaj metode {@link nextChar} so vse
	 * spremenljivke {@link buffChar}, {@link buffCharLine} in
	 * {@link buffCharColumn} namenjene le branju.
	 * 
	 * Vrednost {@code -1} v spremenljivki {@link buffChar} pomeni konec datoteke
	 * (vrednosti spremenljivk {@link buffCharLine} in {@link buffCharColumn} pa
	 * nista ve"c veljavni).
	 */
	private void nextChar() {
		try {
			switch (buffChar) {
			case -2: // Noben znak "se ni bil prebran.
				buffChar = srcFile.read();
				buffCharLine = buffChar == -1 ? 0 : 1;
				buffCharColumn = buffChar == -1 ? 0 : 1;
				return;
			case -1: // Konec datoteke je bil "ze viden.
				return;
			case '\n': // Prejsnji znak je koncal vrstico, zacne se nova vrstica.
				buffChar = srcFile.read();
				buffCharLine = buffChar == -1 ? buffCharLine : buffCharLine + 1;
				buffCharColumn = buffChar == -1 ? buffCharColumn : 1;
				return;
			case '\t': // Prejsnji znak je tabulator, ta znak je morda potisnjen v desno.
				buffChar = srcFile.read();
				while (buffCharColumn % 8 != 0)
					buffCharColumn += 1;
				buffCharColumn += 1;
				return;
			default: // Prejsnji znak je brez posebnosti.
				buffChar = srcFile.read();
				buffCharColumn += 1;
				return;
			}
		} catch (IOException __) {
			throw new Report.Error("Cannot read source file.");
		}
	}

	/**
	 * Trenutni leksikalni simbol.
	 * 
	 * "Ce vrednost spremenljivke {@code buffToken} ni {@code null}, je simbol "ze
	 * prebran iz vhodne datoteke, ni pa "se predan naprej sintaksnemu analizatorju.
	 * Ta simbol je dostopen z metodama {@link peekToken} in {@link takeToken}.
	 */
	private Token buffToken = null;

	/**
	 * Prebere naslednji leksikalni simbol, ki je nato dostopen preko metod
	 * {@link peekToken} in {@link takeToken}.
	 */
	private void nextToken() {
		// Whitespace detection
		while (Character.isWhitespace((char) buffChar)) {
			nextChar();
		}

		// Symbol detection
		switch (buffChar) {
			case '=':
				if (peekChar() == '=') {
					nextChar();
					buffToken = new Token(
							new Report.Location(buffCharLine, buffCharColumn-1,
									buffCharLine, buffCharColumn),
							Token.Symbol.EQU,
							"=="
					);
				} else {
					buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn), Token.Symbol.ASSIGN, "=");
				}
				nextChar(); return;
			case '!':
				if (peekChar() == '=') {
					nextChar();
					buffToken = new Token(
							new Report.Location(buffCharLine, buffCharColumn-1,
									buffCharLine, buffCharColumn),
							Token.Symbol.NEQ,
							"!="
					);
				} else {
					buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn), Token.Symbol.NOT, "!");
				}
				nextChar(); return;
			case '<':
				if (peekChar() == '=') {
					nextChar();
					buffToken = new Token(
							new Report.Location(buffCharLine, buffCharColumn-1,
									buffCharLine, buffCharColumn),
							Token.Symbol.LEQ,
							"<="
					);
				} else {
					buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn), Token.Symbol.LTH, "<");
				}
				nextChar(); return;
			case '>':
				if (peekChar() == '=') {
					nextChar();
					buffToken = new Token(
							new Report.Location(buffCharLine, buffCharColumn-1,
									buffCharLine, buffCharColumn),
							Token.Symbol.GEQ,
							">="
					);
				} else {
					buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn), Token.Symbol.GTH, ">");
				}
				nextChar(); return;
			case '&':
				if (peekChar() == '&') {
					nextChar();
					buffToken = new Token(
							new Report.Location(buffCharLine, buffCharColumn-1,
									buffCharLine, buffCharColumn),
							Token.Symbol.AND,
							"&&"
					);
				} else {
					throw new Report.Error(new Report.Location(buffCharLine, buffCharColumn),"Neprepoznan simbol: &");
				}
				nextChar(); return;
			case '|':
				if (peekChar() == '|') {
					nextChar();
					buffToken = new Token(
							new Report.Location(buffCharLine, buffCharColumn-1,
									buffCharLine, buffCharColumn),
							Token.Symbol.OR,
							"||"
					);
				} else {
					throw new Report.Error(new Report.Location(buffCharLine, buffCharColumn), "Neprepoznan simbol: |");
				}
				nextChar(); return;
			case '+':
				buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn), Token.Symbol.ADD, "+");
				nextChar(); return;
			case '-':
				buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn), Token.Symbol.SUB, "-");
				nextChar(); return;
			case '*':
				buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn), Token.Symbol.MUL, "*");
				nextChar(); return;
			// Special case, might be comment
			case '/':
				if (peekChar() == '/') {
					nextChar();
					while (buffChar != '\n' && buffChar != -1) { nextChar(); }
					if (buffChar != -1) nextChar(); // Consume newline after the comment
					nextToken(); // find the next token after the comment
				} else {
					buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn), Token.Symbol.DIV, "/");
					nextChar();
				} return;
			case '%':
				buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn), Token.Symbol.MOD, "%");
				nextChar(); return;
			case '^':
				buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn), Token.Symbol.PTR, "^");
				nextChar(); return;
			case '(':
				buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn), Token.Symbol.LPAREN, "(");
				nextChar(); return;
			case ')':
				buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn), Token.Symbol.RPAREN, ")");
				nextChar(); return;
			case ',':
				buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn), Token.Symbol.COMMA, ",");
				nextChar(); return;
			case ';':
				buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn), Token.Symbol.SEMIC, ",");
				nextChar(); return;
		}

		// Name and keyword detection
		if (Character.isLetter(buffChar) || buffChar == '_') {
			int startLine = buffCharLine; int startCol = buffCharColumn;

			StringBuilder sb = new StringBuilder();
			sb.append((char) buffChar);

			int nextSymbol = peekChar();
			while (Character.isLetterOrDigit(nextSymbol) || nextSymbol == '_') {
				nextChar();
				sb.append((char) buffChar);
				nextSymbol = peekChar();
			}
			String word = sb.toString();
			Report.Location loc = new Report.Location(startLine, startCol, buffCharLine, buffCharColumn);

			switch (word) {
				case "fun":   buffToken = new Token(loc, Token.Symbol.FUN, word); nextChar(); return;
				case "var":   buffToken = new Token(loc, Token.Symbol.VAR, word); nextChar(); return;
				case "if":    buffToken = new Token(loc, Token.Symbol.IF, word); nextChar(); return;
				case "then":  buffToken = new Token(loc, Token.Symbol.THEN, word); nextChar(); return;
				case "else":  buffToken = new Token(loc, Token.Symbol.ELSE, word); nextChar(); return;
				case "while": buffToken = new Token(loc, Token.Symbol.WHILE, word); nextChar(); return;
				case "do":    buffToken = new Token(loc, Token.Symbol.DO, word); nextChar(); return;
				case "let":   buffToken = new Token(loc, Token.Symbol.LET, word); nextChar(); return;
				case "in":    buffToken = new Token(loc, Token.Symbol.IN, word); nextChar(); return;
				case "end":
					buffToken = new Token(loc, Token.Symbol.END, word); nextChar(); return;
				default:      buffToken = new Token(loc, Token.Symbol.IDENTIFIER, word); nextChar(); return;
			}
		}

		// CONSTANTS
		// Integer detection
		if (Character.isDigit(buffChar)) {
			int startLine = buffCharLine; int startCol = buffCharColumn;

			StringBuilder sb = new StringBuilder();
			sb.append((char) buffChar);

			while (Character.isDigit(peekChar())) {
				nextChar();
				sb.append((char) buffChar);
			}

			buffToken = new Token(
					new Report.Location(startLine, startCol, buffCharLine, buffCharColumn),
					Token.Symbol.INTCONST,
					sb.toString()
			);
			nextChar(); return;
		}

		// Character detection
		if (buffChar == '\'') {
			int startLine = buffCharLine; int startCol = buffCharColumn;

			nextChar();
			StringBuilder sb = new StringBuilder("'");

			if (buffChar < 32 || buffChar > 126) {
				throw new Report.Error(new Report.Location(buffCharLine, buffCharColumn), "Neznana koda znaka");
			} else if (buffChar == '\\') {
				String lexem = verifyEscapeSequence(true);
				sb.append("\\"); sb.append(lexem);
			} else { sb.append((char) buffChar); }

			nextChar();
			if (!(buffChar =='\'')) {
				throw new Report.Error(new Report.Location(buffCharLine, buffCharColumn), "Manjkajoča navednica (')");
			}
			sb.append((char)buffChar);
			buffToken = new Token(
					new Report.Location(startLine, startCol, buffCharLine, buffCharColumn),
					Token.Symbol.CHARCONST,
					sb.toString()
			);
			nextChar(); return;
		}

		// String detection
		if (buffChar == '"') {
			int startLine = buffCharLine; int startCol = buffCharColumn;

			nextChar();
			StringBuilder sb = new StringBuilder("\"");
			while (buffChar != '"' && buffChar != '\n') {
				if (buffChar < 32 || buffChar > 126) {
					throw new Report.Error(new Report.Location(buffCharLine, buffCharColumn), "Neznana koda znaka");
				} else if (buffChar == '\\') {
					String lexem = verifyEscapeSequence(false);
					sb.append("\\"); sb.append(lexem);
					nextChar();
					continue;
				}
				sb.append((char) buffChar);
				nextChar();
			}
			if (!(buffChar =='"')) {
				throw new Report.Error(new Report.Location(buffCharLine, buffCharColumn), "Manjkajoča navednica (\")");
			}
			sb.append((char)buffChar);
			buffToken = new Token(
					new Report.Location(startLine, startCol, buffCharLine, buffCharColumn),
					Token.Symbol.STRINGCONST,
					sb.toString()
			);
			nextChar(); return;
		}

		// check EOF
		if (buffChar == -1) {
			buffToken = new Token(new Report.Location(0, 0), Token.Symbol.EOF, "");
			return;
		}
		throw new Report.Error(new Report.Location(buffCharLine, buffCharColumn), "Neznan leksem: " + buffChar);
	}

	/**
	 * Helper method to look at next character without consuming it
	 * */
	private int peekChar() {
		try {
			srcFile.mark(1);
			int next = srcFile.read();
			srcFile.reset();
			return next;
		} catch (IOException e) {
			return -1;
		}
	}

	/**
	 * Helper method to verify escape sequences in characters and strings
	 * */
	private String verifyEscapeSequence(boolean charMode) {
		nextChar();
		switch (buffChar) {
			case '\\': return "\\";
			case 'n': return "n";
			case '\'': if (charMode) return "'";
			case '"': if (!charMode) return "\"";
		}
		// hex char codes
		if ((Character.isDigit(buffChar)) || buffChar >= 'a' && buffChar <= 'f') {
			char temp = (char) buffChar;
			nextChar();

			if (Character.isDigit(buffChar) || buffChar >= 'a' && buffChar <= 'f') {
				return Character.toString(temp) + (char) buffChar;
			}
		}

		throw new Report.Error(
				new Report.Location(buffCharLine, buffCharColumn),
				"Neprepoznana ubežna sekvenca"
		);
	}

	/**
	 * Vrne trenutni leksikalni simbol, ki ostane v lastnistvu leksikalnega
	 * analizatorja.
	 * 
	 * @return Leksikalni simbol.
	 */
	public Token peekToken() {
		if (buffToken == null)
			nextToken();
		return buffToken;
	}

	/**
	 * Vrne trenutni leksikalni simbol, ki preide v lastnistvo klicoce kode.
	 * 
	 * @return Leksikalni simbol.
	 */
	public Token takeToken() {
		if (buffToken == null)
			nextToken();
		final Token thisToken = buffToken;
		buffToken = null;
		return thisToken;
	}

	// --- ZAGON ---

	/**
	 * Zagon leksikalnega analizatorja kot samostojnega programa.
	 * 
	 * @param cmdLineArgs Argumenti v ukazni vrstici.
	 */
	public static void main(final String[] cmdLineArgs) {
		System.out.println("This is PINS'26 compiler (lexical analysis):");

		try {
			if (cmdLineArgs.length == 0)
				throw new Report.Error("No source file specified in the command line.");
			if (cmdLineArgs.length > 1)
				Report.warning("Unused arguments in the command line.");

			try (LexAn lexAn = new LexAn(cmdLineArgs[0])) {
				while (lexAn.peekToken().symbol() != Token.Symbol.EOF)
					System.out.println(lexAn.takeToken());
				System.out.println(lexAn.takeToken());
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
