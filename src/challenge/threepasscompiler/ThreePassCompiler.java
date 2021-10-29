package challenge.threepasscompiler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

interface Ast {

  String op();
}

class BinOp implements Ast {
  private String operator;
  private Ast a;
  private Ast b;

  public BinOp(String operator, Ast a, Ast b) {
    this.operator = operator;
    this.a = a;
    this.b = b;
  }

  public Ast a() {
    return a;
  }

  public Ast b() {
    return b;
  }

  @Override
  public String op() {
    return operator;
  }

}

class UnOp implements Ast {
  private String operator;
  private int n;

  public UnOp(String operator, int n) {
    this.operator = operator;
    this.n = n;
  }

  public int n() {
    return n;
  }

  @Override
  public String op() {
    return operator;
  }

}

class Compiler {

  public Compiler() {

  }

  public List<String> compile(String prog) {
    return pass3(pass2(pass1(prog)));
  }

  /**
   * Returns an un-optimized AST
   */
  public Ast pass1(String prog) {
    Deque<String> tokens = tokenize(prog);
    List<String> arguments = collectArguments(tokens);
    return expression(tokens, arguments);
  }

  /**
   * Returns an AST with constant expressions reduced
   */
  public Ast pass2(Ast ast) {
    return reduceExpression(ast);
  }

  /**
   * Returns assembly instructions
   */
  public List<String> pass3(Ast ast) {
    return generateAssembly(ast);
  }

  private Deque<String> tokenize(String prog) {
    Deque<String> tokens = new LinkedList<>();
    Pattern pattern = Pattern.compile("[-+*/()\\[\\]]|[a-zA-Z]+|\\d+");
    Matcher m = pattern.matcher(prog);
    while (m.find()) {
      tokens.add(m.group());
    }
    tokens.add("$"); // end-of-stream
    return tokens;
  }

  private List<String> collectArguments(Deque<String> tokens) {
    List<String> arguments = new ArrayList<>();
    tokens.poll(); // skip [
    String token = tokens.poll();
    while (!token.equals("$") && !token.equals("]")) {
      arguments.add(token);
      token = tokens.poll();
    }
    return arguments;
  }

  private Ast expression(Deque<String> tokens, List<String> arguments) {
    Ast termA = term(tokens, arguments);
    Ast exp = null;
    String token = tokens.peek();

    if (token.equals("$") || (!token.equals("+") && !token.equals("-"))) {
      return termA;
    }

    while (!token.equals("$") && (token.equals("+") || token.equals("-"))) {
      String operator = tokens.poll();
      Ast termB = term(tokens, arguments);
      if (exp != null) {
        exp = new BinOp(operator, exp, termB);
      } else {
        exp = new BinOp(operator, termA, termB);
      }
      token = tokens.peek();
    }

    return exp;
  }

  private Ast term(Deque<String> tokens, List<String> arguments) {
    Ast factorA = factor(tokens, arguments);
    Ast term = null;
    String token = tokens.peek();

    if (token.equals("$") || (!token.equals("*") && !token.equals("/"))) {
      return factorA;
    }
    while (!token.equals("$") && (token.equals("*") || token.equals("/"))) {
      String operator = tokens.poll();
      Ast factorB = factor(tokens, arguments);
      if (term != null) {
        term = new BinOp(operator, term, factorB);
      } else {
        term = new BinOp(operator, factorA, factorB);
      }
      token = tokens.peek();
    }
    return term;
  }

  private Ast factor(Deque<String> tokens, List<String> arguments) {
    String token = tokens.poll();
    try {
      int n = Integer.parseInt(token);
      return new UnOp("imm", n);
    } catch (NumberFormatException e) {
    }
    if (token.equals("(")) {
      Ast expression = expression(tokens, arguments);
      tokens.poll(); // skip )
      return expression;
    }

    return new UnOp("arg", arguments.indexOf(token));
  }

  private Ast reduceExpression(Ast expression) {
    String operator = expression.op();
    if (operator.equals("imm") || operator.equals("arg")) {
      return expression;
    }
    Ast expA = reduceExpression(((BinOp) expression).a());
    Ast expB = reduceExpression(((BinOp) expression).b());

    if (expA.op().equals("imm") && expB.op().equals("imm")) {

      int result = 0;
      switch (operator) {
        case "+": {
          result = ((UnOp) expA).n() + ((UnOp) expB).n();
          break;
        }
        case "-": {
          result = ((UnOp) expA).n() - ((UnOp) expB).n();
          break;
        }
        case "*": {
          result = ((UnOp) expA).n() * ((UnOp) expB).n();
          break;
        }
        case "/": {
          result = ((UnOp) expA).n() / ((UnOp) expB).n();
          break;
        }
      }
      return new UnOp("imm", result);

    }

    return new BinOp(operator, expA, expB);
  }

  private List<String> generateAssembly(Ast expression) {
    List<String> assemblyInstruction = new ArrayList<>();

    if (expression.op().equals("imm")) {
      assemblyInstruction.add("IM " + ((UnOp) expression).n());
    } else if (expression.op().equals("arg")) {
      assemblyInstruction.add("AR " + ((UnOp) expression).n());
    } else {
      assemblyInstruction = this.generateAssembly(((BinOp) expression).a());
      assemblyInstruction.addAll(generateAssembly(((BinOp) expression).b()));
      assemblyInstruction.addAll(Arrays.asList("PO", "SW", "PO"));
      switch (expression.op()) {
        case "+": {
          assemblyInstruction.add("AD");
          break;
        }
        case "-": {
          assemblyInstruction.add("SU");
          break;
        }
        case "*": {
          assemblyInstruction.add("MU");
          break;
        }
        case "/": {
          assemblyInstruction.add("DI");
          break;
        }
      }
    }
    assemblyInstruction.add("PU");
    return assemblyInstruction;
  }
}

class Simulator {
  public static int simulate(List<String> asm, int... argv) {
    int r0 = 0;
    int r1 = 0;
    Deque<Integer> stack = new LinkedList<>();
    for (String ins : asm) {
      String code = ins.replaceAll("\\s+[0-9]+", "");
      switch (code) {
        case "IM":
          r0 = Integer.parseInt(ins.substring(2).trim());
          break;
        case "AR":
          r0 = argv[Integer.parseInt(ins.substring(2).trim())];
          break;
        case "SW":
          int tmp = r0;
          r0 = r1;
          r1 = tmp;
          break;
        case "PU":
          stack.addLast(r0);
          break;
        case "PO":
          r0 = stack.removeLast();
          break;
        case "AD":
          r0 += r1;
          break;
        case "SU":
          r0 -= r1;
          break;
        case "MU":
          r0 *= r1;
          break;
        case "DI":
          r0 /= r1;
          break;
      }
    }
    return r0;
  }
}

public class ThreePassCompiler {

  public static void main(String[] args) {
    String program = "[ x y z ] ( 2*3*x + 5*y - 3*z ) / (1 + 3 + 2*2)";
    Compiler compiler = new Compiler();
    Ast pass1 = compiler.pass1(program);
    Ast pass2 = compiler.pass2(pass1);
    List<String> p3 = compiler.pass3(pass2);
    System.out.println(
        String.format("Expected: prog(4,0,0) == 3 -> Result: %s", Simulator.simulate(p3, 4, 0, 0)));
    System.out.println(
        String.format("Expected: prog(4,8,0) == 8 -> Result: %s", Simulator.simulate(p3, 4, 8, 0)));
    System.out.println(String.format("Expected: prog(4,8,16) == 2 -> Result: %s",
        Simulator.simulate(p3, 4, 8, 16)));
  }

}
