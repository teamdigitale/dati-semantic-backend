package it.teamdigitale.ndc;

import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.sparql.lang.sparql_11.ParseException;

public class Runner {

    public static void main(String[] args) throws ParseException {
        String updateUrl = "http://localhost:8890/sparql-graph-crud";
        String url = "http://localhost:8890/sparql";

        RDFConnection conn = RDFConnectionFactory.connect(url);
        //delete whole graph
//        conn.delete("http://ndc.com/jena");
//        //dump local ttl file in named graph
//        conn.load("http://ndc.com/jena",
//            "./sample-data/sample.ttl");

        //simple select query
        Query build = new SelectBuilder()
            .addPrefix("lnt", "http://looneytunes-graph.com/")
            .setDistinct(true).addVar("?s")
            .addWhere("?s", "lnt:name", "Bugs Bunny")
            .build();
        String query = build.toString();

        System.out.println(query);

        QueryExecution queryExecution = QueryExecutionFactory.sparqlService(url, query);
        queryExecution.execSelect()
            .forEachRemaining(querySolution -> System.out.println(querySolution.get("s")));

        System.out.println("Using conn.querySelect");
        conn.querySelect(query, querySolution -> System.out.println(querySolution.get("s")));


        //SELECT ?c ?p WHERE { ?p ^:created_by ?c }
        Query inversePath = new SelectBuilder()
            .addPrefix("lnt", "http://looneytunes-graph.com/")
            .addVar("?c").addVar("?p")
            .addWhere("?c", "lnt:created_by", "?p")
            .build();

        System.out.println("===========================");
        System.out.println(inversePath.toString());
        System.out.println("===========================");
        conn.querySelect(inversePath, querySolution -> System.out.println(
            querySolution.get("c") + "-" + querySolution.get("p")));

        //SELECT ?age WHERE { :Tex_Avery :born_on ?b ; :died_on ?d . BIND (year(?b) AS ?bYear)
        // BIND (year(?d) AS ?dYear) BIND (?dYear - ?bYear AS ?age) }
        SelectBuilder builder = new SelectBuilder()
                .addPrefix("lnt", "http://looneytunes-graph.com/");
        ExprFactory exprFactory=builder.getExprFactory();
        Query bind = builder.addVar("?age")
                .addWhere("lnt:Tex_Avery", "lnt:born_on", "?b")
                .addWhere("lnt:Tex_Avery", "lnt:died_on", "?d")
                .addBind(exprFactory.year("?b"), "?bYear")
                .addBind(exprFactory.year("?d"), "?dYear")
                .addBind(exprFactory.subtract("?dYear","?bYear") , "?age")
                .build()  ;
        System.out.println("============BIND===============");
        System.out.println(bind.toString());
        conn.querySelect(bind, s -> System.out.println(s.get("age").asLiteral().getInt()));
        System.out.println("==============BIND=============");

//        SELECT ?c1 ?c2
//        WHERE
//        {
//?c1 :enemy_of/:rival_of ?c2
//        }
        Query sequencePathQuery = new SelectBuilder()
                .addPrefix("lnt", "http://looneytunes-graph.com/")
                .addVar("?c1")
                .addVar("?c2")
                .addWhere("?c1", "lnt:enemy_of/lnt:rival_of", "?c2")
                .build();
        System.out.println("============sequencePathQuery===============");
        conn.querySelect(sequencePathQuery, querySolution -> System.out.println(querySolution.get("c1")+" "+querySolution.get("c2")));
        System.out.println("============sequencePathQuery===============");
    }
}
