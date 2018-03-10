SWAGGERJAR = $$HOME/.m2/repository/io/swagger/swagger-codegen-cli/2.4.0-SNAPSHOT/swagger-codegen-cli-2.4.0-SNAPSHOT.jar
SPEC = "http://petstore.swagger.io/v2/swagger.json"
clean:
	rm -rf samples_*
jar: meta
	cd output;mvn package
clj:
	java -jar $(SWAGGERJAR) generate -i $(SPEC) -l clojure -o samples_java
meta: output
	touch meta
output:
	java -jar $(SWAGGERJAR) meta -o output -n common-lisp-client

cl: output
	java -cp output/target/common-lisp-client-swagger-codegen-1.0.0.jar:$(SWAGGERJAR) io.swagger.codegen.SwaggerCodegen \
	generate -i $(SPEC) -l common-lisp-client -o samples_cl
