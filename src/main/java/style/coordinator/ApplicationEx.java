package style.coordinator;

import static org.eclipse.jdt.internal.compiler.parser.TerminalTokens.TokenNameCOMMENT_LINE;
import static org.eclipse.jdt.internal.compiler.parser.TerminalTokens.TokenNameEOF;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.formatter.DefaultCodeFormatterOptions;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class ApplicationEx {
  private final static Logger log = LoggerFactory.getLogger(ApplicationEx.class);

  public static void main(String[] args)
      throws IOException, InvalidInputException, ParserConfigurationException, SAXException {

    String inputSourceCode;
    StringBuilder sb = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(new FileReader(args[0]))) {
      String string = reader.readLine();
      while (string != null){
        sb.append(string + System.getProperty("line.separator"));
        string = reader.readLine();
      }
    }

    inputSourceCode = sb.toString();
    String outputSourceCode;
      try {
        inputSourceCode = breakStyle(inputSourceCode);
      } catch (Exception e) {
        log.error("--------");
        log.error("parsing error", e);
        log.error(inputSourceCode);
        throw e;
      }
      outputSourceCode = format(inputSourceCode, readStyle(new File(args[0])));

      String outputPath = args[1];

      try{
          File file = new File(args[1]);
          FileWriter fw= new FileWriter(file);
          fw.write(outputSourceCode);
          fw.close();
      }catch(IOException e){
          System.out.println(e);
      }
  }

  private static String tokenize(String source, String lineSeparator) throws InvalidInputException {
    Objects.requireNonNull(source);
    Objects.requireNonNull(lineSeparator);
    Scanner scanner = new Scanner();
    scanner.setSource(source.toCharArray());
    scanner.recordLineSeparator = false;
    scanner.sourceLevel = ClassFileConstants.JDK1_8;
    scanner.tokenizeWhiteSpace = false;
    StringBuilder sb = new StringBuilder();
    while (scanner.getNextToken() != TokenNameEOF) {
      sb.append(scanner.getCurrentTokenSource());
      sb.append(lineSeparator);
    }
    return sb.toString();
  }

  private static Map<String, String> readStyle(File file)
      throws ParserConfigurationException, SAXException, IOException {
    Objects.requireNonNull(file);
    // take default Eclipse formatting options
    Map<String, String> options = DefaultCodeFormatterOptions.getEclipseDefaultSettings().getMap();
    // initialize the compiler settings to be able to format 1.8 code
    options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_8);
    options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_8);
    options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8);
    // 空行が全部消える
    // コメント行の前の改行が消される
    // コメント行の後ろに絶対改行が入る
    options.put("org.eclipse.jdt.core.formatter.number_of_empty_lines_to_preserve","1");
    options.put("org.eclipse.jdt.core.formatter.indent_empty_lines","false");
    return options;
  }

  public static String breakStyle(String source) throws InvalidInputException {
    Objects.requireNonNull(source);
    Scanner scanner = new Scanner();
    scanner.setSource(source.toCharArray());
    scanner.recordLineSeparator = false;
    scanner.sourceLevel = ClassFileConstants.JDK1_8;
    scanner.tokenizeWhiteSpace = false;
    scanner.tokenizeComments = true;
    StringBuilder sb = new StringBuilder();
    int tokenType;
    while ((tokenType = scanner.getNextToken()) != TokenNameEOF) {
      int start = scanner.getCurrentTokenStartPosition();
      int end = scanner.getCurrentTokenEndPosition();
      sb.append(source.substring(start, end + 1));
      if (tokenType == TokenNameCOMMENT_LINE) {
        sb.append("\n");
      } else {
        sb.append(" ");
      }
    }
    return sb.toString();
  }

  private static String format(String source, Map<String, String> style)
      throws IOException, ParserConfigurationException, SAXException {
    Objects.requireNonNull(source);
    Objects.requireNonNull(style);
    // instantiate the default code formatter with the given options
    final CodeFormatter codeFormatter = ToolFactory.createCodeFormatter(style);

    // retrieve the source to format
    String separator = System.getProperty("line.separator");
    final TextEdit edit;
    try {
      edit = codeFormatter.format(CodeFormatter.K_COMPILATION_UNIT, // format a compilation unit
          source, // source to format
          0, // starting position
          source.length(), // length
          0, // initial indentation
          separator // line separator
      );
    } catch (RuntimeException e) {
      log.error("--------");
      log.error("formatting error", e);
      log.error(source);
      throw e;
    }

    IDocument document = new Document(source);
    try {
      edit.apply(document);
    } catch (MalformedTreeException | BadLocationException e) {
      e.printStackTrace();
    }

    return document.get();
  }
}
