/*
 * Copyright (c) 2011 - Georgios Gousios <gousiosg@gmail.com>
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package gr.gousiosg.javacg.stat;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.bcel.classfile.ClassParser;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.Result;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.TransactionWork;
//import org.neo4j.graphdb.Node;

import static org.neo4j.driver.Values.parameters;
// end::hello-world-import[]

// tag::hello-world[]

/**
 * Constructs a callgraph out of a JAR archive. Can combine multiple archives
 * into a single call graph.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
public class JCallGraph implements AutoCloseable {

	public static void main(String[] args) throws Exception {

		String results = " ";
		Function<ClassParser, ClassVisitor> getClassVisitor = (ClassParser cp) -> {
			try {
				return new ClassVisitor(cp.parse());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		};

		try {
			for (String arg : args) {

				File f = new File(arg);

				if (!f.exists()) {
					System.err.println("Jar file " + arg + " does not exist");
				}

				try (JarFile jar = new JarFile(f)) {
					Stream<JarEntry> entries = enumerationAsStream(jar.entries());

					String methodCalls = entries.flatMap(e -> {
						if (e.isDirectory() || !e.getName().endsWith(".class"))
							return (new ArrayList<String>()).stream();

						ClassParser cp = new ClassParser(arg, e.getName());
						return getClassVisitor.apply(cp).start().methodCalls().stream();
					}).map(s -> s + "\n").reduce(new StringBuilder(), StringBuilder::append, StringBuilder::append)
							.toString();
					results = methodCalls;

					// BufferedWriter log = new BufferedWriter(new OutputStreamWriter(System.out));
					FileWriter fw = new FileWriter(new File("d:/cloudify/jcg.txt"));
					BufferedWriter log = new BufferedWriter(fw);

					log.write(methodCalls);

					System.out.println(methodCalls);
					log.close();

				}

			}
		} catch (IOException e) {
			System.err.println("Error while processing jar: " + e.getMessage());
			e.printStackTrace();
		}

		try (JCallGraph greeter = new JCallGraph("bolt://localhost:7687", "neo4j", "neo4j")) {
			results = readFileAsString("d:/cloudify/jcg.txt");
			greeter.addtoDB(results);
		} catch (Exception e) {
			System.out.println("Some thing is missing");
		}

	}

	public static String readFileAsString(String fileName) throws Exception {
		String data = " ";
		data = new String(Files.readAllBytes(Paths.get(fileName)));
		return data;
	}

	public void addtoDB(String methodCalls)
   {
	   try {
		   HashMap<String ,ArrayList<String>> data = new HashMap<>();
			ArrayList<String> function = new ArrayList<>();
	
   	for(String s:methodCalls.split("\n"))
   		for(String str:methodCalls.split("\\s")){
   	    {
   		String[] controls = str.split(":");
   		   		if(!controls[1].contains("controller")) {
   			continue;
   		}
   		String className = controls[1];
   		String methodName = controls[2];
   		
   		if(!data.containsKey(className))
   		{
   			data.put(className,new ArrayList<String>());
   			data.get(className).add(methodName);
   		}
   		    data.get(className).add(methodName);
   		}
	}
   		writeInNeo4j(data);
	   }
   	
   		
   		catch(Exception e) {
   			System.out.println("Some thing is wrong with input!\n"+methodCalls+ e.getMessage());
   			e.printStackTrace();
   		}
   	
   }

	public static <T> Stream<T> enumerationAsStream(Enumeration<T> e) {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<T>() {
			public T next() {
				return e.nextElement();
			}

			public boolean hasNext() {
				return e.hasMoreElements();
			}
		}, Spliterator.ORDERED), false);
	}

	private final Driver driver;

	public JCallGraph(String uri, String user, String password) {
		driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
	}

	public void close() throws Exception {
		driver.close();
	}
	
//	  private static void callwebapi()
//      {
//          String apiUrl = "https://localhost:44399/generateapp";
//
//          WebClient client = new WebClient();
//          //client.Headers["Content-type"] = "application/json";
//          //client.Encoding = Encoding.UTF8;
//          client.UploadFile(apiUrl, "D:\\LathaProjects\\micropoc\\test.json");
//
//      }

	public void writeInNeo4j(HashMap<String , ArrayList<String>> data) throws Exception
    {
   	
        try ( Session session = driver.session() )
      {           
           
          session.writeTransaction(new TransactionWork<Void>()
            {
            	
            	@Override
            	public Void execute (Transaction cv)
            	{
            		
            		 int i =1;
            		 for(String className: data.keySet()){
            			 
            			 System.out.println(className);
            			 cv.run( " MERGE (c:Class {name: $className})" + 
             					 " ON CREATE SET c.location = 'random location'",parameters("className",className));
            			
            		 for (String methodName : data.get(className)){
            			 
            			 	cv.run("WITH $methodName AS implements" +
            			 			" MATCH (c:Class{name: $className})" +
            			 			" MERGE (m:method{name:implements })" +
            			 			"MERGE (m)-[:HasFunction]->(c)"+
//									"WITH $methodName AS implements" +
//            			 			"MATCH (child:method) MATCH (parent:Class)"+
//            			 			"MERGE (parent)-[:HAS_CHILD]->(child)"+
            			 			
            			 			" ON CREATE SET m.link = 'http://localhost:8080/'+$className+'/'+$methodName",parameters("methodName",methodName,"className",className));
            		
            		 
            		 	}
            		}		 				
                             return null;
            	}
           });
              
            System.out.println( data );
           
        }
    }
}

// try {
//
// Scanner sc = new Scanner(new File(fw));
//
// System.out.println(sc);
// TreeSet<String> ts1 = new TreeSet<String>();
// TreeSet<String> ts2 = new TreeSet<String>();
//
// while(sc.hasNextLine()){
// String line = sc.nextLine();
// String[] details = line.split(":");
// String classes = details[0];
// String methods = details[1];
//
// ts1.add(classes);
// ts2.add(methods);
//
// }
//
// for(String classes: ts1){
//
//
// System.out.println(classes.toString());
// }
//
// for(String methods: ts2){
//
//
// System.out.println(methods.toString());
// }
//
//
//
// } catch (FileNotFoundException e) {
// e.printStackTrace();
// }
//
