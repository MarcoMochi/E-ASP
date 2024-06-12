package application.antlr4;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import application.antlr4.generated.*;

import java.util.HashSet;
import java.util.Set;

public class Reader {
	
	 public static Set<String> search(String rule) throws Exception {
    	
    	CharStream input = CharStreams.fromString(rule);

        ASPCore2Lexer lexer = new ASPCore2Lexer(input);

        CommonTokenStream tokens = new CommonTokenStream(lexer);

        ASPCore2Parser parser = new ASPCore2Parser(tokens);
        ParseTree tree = parser.program();

        VariableExtractor extractor = new VariableExtractor();
        Set<String> variables = extractor.visit(tree);

        return variables;
    }
}

class VariableExtractor extends ASPCore2BaseVisitor<Set<String>> {
	 @Override
	    public Set<String> visitProgram(ASPCore2Parser.ProgramContext ctx) {
	        Set<String> variables = new HashSet<>();
	        if (ctx.statements() != null) {
	            variables.addAll(visit(ctx.statements()));
	        }
	        if (ctx.query() != null) {
	            variables.addAll(visit(ctx.query()));
	        }
	        return variables;
	    }

	    @Override
	    public Set<String> visitStatements(ASPCore2Parser.StatementsContext ctx) {
	        Set<String> variables = new HashSet<>();
	        for (ASPCore2Parser.StatementContext statementContext : ctx.statement()) {
	        	variables.addAll(visit(statementContext));
	        }
	        return variables;
	    }

	    @Override
	    public Set<String> visitStatement_rule(ASPCore2Parser.Statement_ruleContext ctx) {
	        Set<String> variables = new HashSet<>();
	        variables.addAll(visit(ctx.body()));
	        return variables;
	    }

	    @Override
	    public Set<String> visitBody(ASPCore2Parser.BodyContext ctx) {
	        Set<String> variables = new HashSet<>();
	        if (ctx.naf_literal() != null) {
	            variables.addAll(visit(ctx.naf_literal()));
	        }
	        
	        if (ctx.body() != null) {
	            variables.addAll(visit(ctx.body()));
	        }
	        return variables;
	    }

	    @Override
	    public Set<String> visitDisjunction(ASPCore2Parser.DisjunctionContext ctx) {
	        Set<String> variables = new HashSet<>();
	        ASPCore2Parser.Classical_literalContext classicalLiteralContext = ctx.classical_literal();
	        variables.addAll(visit(classicalLiteralContext));
	        return variables;
	    }

	    @Override
	    public Set<String> visitClassical_literal(ASPCore2Parser.Classical_literalContext ctx) {
	        return visit(ctx.basic_atom());
	    }

	    @Override
	    public Set<String> visitBasic_atom(ASPCore2Parser.Basic_atomContext ctx) {
	        Set<String> variables = new HashSet<>();
	        if (ctx.id() != null && ctx.terms() == null) {
	            variables.add(ctx.id().getText());
	        }
	        if (ctx.terms() != null) {
	            variables.addAll(visit(ctx.terms()));
	        }
	        return variables;
	    }

	    @Override
	    public Set<String> visitTerms(ASPCore2Parser.TermsContext ctx) {
	        Set<String> variables = new HashSet<>();
	        ASPCore2Parser.TermContext termContext = ctx.term();
	        variables.addAll(visit(termContext));
	        return variables;
	    }

	    @Override
	    public Set<String> visitTerm_variable(ASPCore2Parser.Term_variableContext ctx) {
	        Set<String> variables = new HashSet<>();
	        variables.add(ctx.VARIABLE().getText());
	        return variables;
	   }
}
       