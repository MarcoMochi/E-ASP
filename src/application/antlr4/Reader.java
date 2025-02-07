package application.antlr4;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import application.antlr4.generated.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Reader {
	
	 public static Map<String, List<String>> search(String rule) throws Exception {
		
		if (rule.split(":-").length > 1)
			rule = "temp :- " + rule.split(":-")[1];
		else
			rule = "temp :- " + rule.split(":-")[0];
		
		
    	CharStream input = CharStreams.fromString(rule);

        ASPCore2Lexer lexer = new ASPCore2Lexer(input);

        CommonTokenStream tokens = new CommonTokenStream(lexer);

        ASPCore2Parser parser = new ASPCore2Parser(tokens);
        ParseTree tree = parser.program();

        VariableExtractor extractor = new VariableExtractor();
        Map<String, List<String>> variables = extractor.visit(tree);

        return variables;
    }
	 
	 public static String get_body(String rule) throws Exception {
		 
		 	CharStream input = CharStreams.fromString(rule);

	        ASPCore2Lexer lexer = new ASPCore2Lexer(input);

	        CommonTokenStream tokens = new CommonTokenStream(lexer);

	        ASPCore2Parser parser = new ASPCore2Parser(tokens);
	        ParseTree tree = parser.program();

	        
	        WeightedStatementBodyVisitor visitor = new WeightedStatementBodyVisitor();
	        visitor.visit(tree);
	        String body = visitor.get_body();
	        return body;
	    }
	 
	 public static String get_cost(String rule) throws Exception {
		 
		 	CharStream input = CharStreams.fromString(rule);

	        ASPCore2Lexer lexer = new ASPCore2Lexer(input);

	        CommonTokenStream tokens = new CommonTokenStream(lexer);

	        ASPCore2Parser parser = new ASPCore2Parser(tokens);
	        ParseTree tree = parser.program();

	        
	        WeightedStatementBodyVisitor visitor = new WeightedStatementBodyVisitor();
	        visitor.visit(tree);
	        String cost = visitor.get_cost();
	        return cost;
	    }


}

class WeightedStatementBodyVisitor extends ASPCore2BaseVisitor {
	
	private String body;
	private String cost;
	
        @Override
        public Void visitStatement_weightConstraint(ASPCore2Parser.Statement_weightConstraintContext ctx) {
        	String bodyText = "";
        	if (ctx.weight_at_level() != null)
        		this.cost = ctx.weight_at_level().getText();
            ASPCore2Parser.BodyContext bodyContext = ctx.body();
            if (bodyContext != null) {
                bodyText = bodyContext.getText();
                this.body = bodyText;
            }
			return null;
        }
        
        public String get_body() {
        	return this.body;
        }
        
        public String get_cost() {
        	return this.cost;
        }
    }


class VariableExtractor extends ASPCore2BaseVisitor<Map<String, List<String>>> {
	
	private Integer cost;
	
	@Override
    public Map<String, List<String>>  visitProgram(ASPCore2Parser.ProgramContext ctx) {
		Map<String, List<String>> variables = new HashMap<>();
        if (ctx.statements() != null) {
            variables.putAll(visit(ctx.statements()));
        }
        if (ctx.query() != null) {
            variables.putAll(visit(ctx.query()));
        }
        return variables;
    }

    @Override
    public Map<String, List<String>>  visitStatements(ASPCore2Parser.StatementsContext ctx) {
    	Map<String, List<String>> variables = new HashMap<>();
        for (ASPCore2Parser.StatementContext statementContext : ctx.statement()) {
        	variables.putAll(visit(statementContext));
        }
        return variables;
    }
    
    @Override
    public Map<String, List<String>> visitStatement_weightConstraint(ASPCore2Parser.Statement_weightConstraintContext ctx) {
    	// Extract the cost from the weight_at_level context
        if (ctx.weight_at_level() != null) {
            visit(ctx.weight_at_level());
        }
        return visit(ctx.body());
    }
    
    @Override
    public Map<String, List<String>> visitWeight_at_level(ASPCore2Parser.Weight_at_levelContext ctx) {
        // Extract the cost (first term in weight_at_level)
        if (ctx.term() != null && !ctx.term().isEmpty()) {
            ASPCore2Parser.TermContext costTerm = ctx.term(0);
            
            if (costTerm != null) {
                // Assuming the cost is a numeric value
                this.cost = Integer.parseInt(costTerm.getText());
            }
        }
        Map<String, List<String>> variables = new HashMap<>();
        return variables;
    }

    public Integer getCost() {
        return this.cost;
    }

    @Override
    public Map<String, List<String>> visitStatement_rule(ASPCore2Parser.Statement_ruleContext ctx) {
    	Map<String, List<String>> variables = new HashMap<>();
    	if (ctx.body() != null)
    		variables.putAll(visit(ctx.body()));
        return variables;
    }
    
	@Override
	public Map<String, List<String>> visitBody(ASPCore2Parser.BodyContext ctx) {
        Map<String, List<String>> variables = new HashMap<>();
        if (ctx.naf_literal() != null) {
            variables.putAll(visit(ctx.naf_literal()));
        }
        if (ctx.body() != null) {
            variables.putAll(visit(ctx.body()));
        }
        return variables;
    }

    @Override
    public Map<String, List<String>> visitNaf_literal(ASPCore2Parser.Naf_literalContext ctx) {
        Map<String, List<String>> variables = new HashMap<>();
        if (ctx.classical_literal() != null) {
            variables.putAll(visit(ctx.classical_literal()));
        }
        if (ctx.external_atom() != null) {
            variables.putAll(visit(ctx.external_atom()));
        }
        if (ctx.builtin_atom() != null) {
            variables.putAll(visit(ctx.builtin_atom()));
        } 
        return variables;
    }
    
    
    
    @Override
    public Map<String, List<String>> visitClassical_literal(ASPCore2Parser.Classical_literalContext ctx) {
        Map<String, List<String>> variables = new HashMap<>();
        if (ctx.basic_atom() != null) {
            variables.putAll(visit(ctx.basic_atom()));
        }
        return variables;
    }

    @Override
    public Map<String, List<String>> visitBasic_atom(ASPCore2Parser.Basic_atomContext ctx) {
        Map<String, List<String>> variables = new HashMap<>();
        if (ctx.id() != null && ctx.terms() != null) {
            // Extract the function name
            String functionName = ctx.id().getText();
            
            List<String> termList = new ArrayList<>();
            Map<String, List<String>> terms = visitTerms(ctx.terms());
            terms.values().forEach(termList::addAll);
            // For each term, map the function name to the term
            variables.put(functionName, termList);  
        }
        return variables;
    }

    @Override
    public Map<String, List<String>> visitTerms(ASPCore2Parser.TermsContext ctx) {
        Map<String, List<String>> variables = new HashMap<>();
        
        while (ctx != null && ctx.term() != null) {
        	ASPCore2Parser.TermContext termContext = ctx.term();
            Map<String, List<String>> termResult = visit(termContext);
            if (termResult != null)
            	termResult.forEach((key, value) -> variables.computeIfAbsent(key, k -> new ArrayList<>()).addAll(value));
            ctx = ctx.terms();
        }
        return variables;
    }

    @Override
	public Map<String, List<String>> visitTerm_string(ASPCore2Parser.Term_stringContext ctx) {
    	Map<String, List<String>> variables = new HashMap<>();
    	if (ctx.QUOTED_STRING() != null) {
    		variables.computeIfAbsent("", k -> new ArrayList<>()).add(ctx.QUOTED_STRING().getText().replace("\\\"", "'"));
    	}
    	return variables;
	}
    
    @Override
    public Map<String, List<String>> visitTerm_variable(ASPCore2Parser.Term_variableContext ctx) {
        Map<String, List<String>> variables = new HashMap<>();
        if (ctx.VARIABLE() != null) {
            variables.computeIfAbsent("", k -> new ArrayList<>()).add(ctx.VARIABLE().getText());
        }
        return variables;
    }
    
    @Override
    public Map<String, List<String>> visitTerm_number(ASPCore2Parser.Term_numberContext ctx) {
        return visitChildren(ctx);
    }
    
    @Override public Map<String, List<String>> visitNumeral(ASPCore2Parser.NumeralContext ctx) { 
    	Map<String, List<String>> variables = new HashMap<>();
        if (ctx.NUMBER() != null) {
        	variables.computeIfAbsent("", k -> new ArrayList<>()).add(ctx.NUMBER().getText());
        }
        return variables;
    }
    

    @Override
    public Map<String, List<String>> visitAggregate(ASPCore2Parser.AggregateContext ctx) {
        Map<String, List<String>> variables = new HashMap<>();
        if (ctx.aggregate_elements() != null) {
            variables.putAll(visit(ctx.aggregate_elements()));
        }
        return variables;
    }

    @Override
    public Map<String, List<String>> visitAggregate_elements(ASPCore2Parser.Aggregate_elementsContext ctx) {
        Map<String, List<String>> variables = new HashMap<>();
        if (ctx.aggregate_element() != null) {
            ASPCore2Parser.Aggregate_elementContext elementContext = ctx.aggregate_element();
            variables.putAll(visit(elementContext));
        }
        return variables;
    }

    @Override
    public Map<String, List<String>> visitExternal_atom(ASPCore2Parser.External_atomContext ctx) {
        Map<String, List<String>> variables = new HashMap<>();
        if (ctx.input != null) {
            variables.putAll(visit(ctx.input));
        }
        if (ctx.output != null) {
            variables.putAll(visit(ctx.output));
        }
        return variables;
    }
}
       