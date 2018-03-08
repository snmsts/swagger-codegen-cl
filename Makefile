SWAGGERJAR = $$HOME/.m2/repository/io/swagger/swagger-codegen-cli/2.4.0-SNAPSHOT/swagger-codegen-cli-2.4.0-SNAPSHOT.jar

clean:
	rm -rf cl_samples/ clj_samples/
jar: meta
	cd output;mvn package
clj:
	java -jar $(SWAGGERJAR) generate -i "http://petstore.swagger.io/v2/swagger.json" -l clojure -o clj_samples
meta: output

output:
	java -jar $(SWAGGERJAR) meta -o output -n common-lisp-client

cl: output
	java -cp output/target/common-lisp-client-swagger-codegen-1.0.0.jar:$(SWAGGERJAR) io.swagger.codegen.SwaggerCodegen \
	generate -i http://petstore.swagger.io/v2/swagger.json -l common-lisp-client -o cl_samples
