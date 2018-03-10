package io.swagger.codegen;
// package io.swagger.codegen.languages;

import io.swagger.codegen.CliOption;
import io.swagger.codegen.CodegenConfig;
import io.swagger.codegen.CodegenConstants;
import io.swagger.codegen.CodegenOperation;
import io.swagger.codegen.CodegenType;
import io.swagger.codegen.DefaultCodegen;
import io.swagger.codegen.SupportingFile;

import io.swagger.models.properties.*;
import io.swagger.models.Swagger;
import io.swagger.models.Info;
import io.swagger.models.Contact;
import io.swagger.models.License;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.List;
import java.io.File;

public class CommonLispClientGenerator extends DefaultCodegen implements CodegenConfig {
  private static final String PROJECT_NAME = "projectName";
  private static final String PROJECT_DESCRIPTION = "projectDescription";
  private static final String PROJECT_VERSION = "projectVersion";
  private static final String PROJECT_URL = "projectUrl";
  private static final String PROJECT_LICENSE_NAME = "projectLicenseName";
  private static final String PROJECT_LICENSE_URL = "projectLicenseUrl";
  private static final String BASE_NAMESPACE = "baseNamespace";

  protected String projectName;
  protected String projectDescription;
  protected String projectVersion;
  protected String baseNamespace;

  protected String sourceFolder = "src";
  protected String apiVersion = "1.0.0";

@Override    
  public CodegenType getTag() {
    return CodegenType.CLIENT;
  }

@Override
  public String getName() {
    return "common-lisp-client";
  }

@Override
  public String getHelp() {
    return "Generates a common-lisp-client client library.";
  }

  public CommonLispClientGenerator() {
    super();
    outputFolder = "generated-code" + File.separator + "common-lisp-client";
    apiTemplateFiles.put("api.mustache", ".lisp");
    embeddedTemplateDir = templateDir = "common-lisp-client";

    cliOptions.add(new CliOption(PROJECT_NAME,"name of the project (Default: generated from info.title or \"swagger-cl-client\")"));
    cliOptions.add(new CliOption(PROJECT_DESCRIPTION,"description of the project (Default: using info.description or \"Client library of <projectNname>\")"));
    cliOptions.add(new CliOption(PROJECT_VERSION,"version of the project (Default: using info.version or \"1.0.0\")"));
    cliOptions.add(new CliOption(PROJECT_URL,"URL of the project (Default: using info.contact.url or not included in .asd file)"));
    cliOptions.add(new CliOption(PROJECT_LICENSE_NAME,"name of the license the project uses (Default: using info.license.name or not included in .asd file)"));
    cliOptions.add(new CliOption(PROJECT_LICENSE_URL,"URL of the license the project uses (Default: using info.license.url or not included in .asd file)"));
    cliOptions.add(new CliOption(BASE_NAMESPACE,"the base/top namespace (Default: generated from projectName)"));
  }
  @Override
  public void preprocessSwagger(Swagger swagger) {
      super.preprocessSwagger(swagger);

      if (additionalProperties.containsKey(PROJECT_NAME)) {
          projectName = ((String) additionalProperties.get(PROJECT_NAME));
      }
      if (additionalProperties.containsKey(PROJECT_DESCRIPTION)) {
          projectDescription = ((String) additionalProperties.get(PROJECT_DESCRIPTION));
      }
      if (additionalProperties.containsKey(PROJECT_VERSION)) {
          projectVersion = ((String) additionalProperties.get(PROJECT_VERSION));
      }
      if (additionalProperties.containsKey(BASE_NAMESPACE)) {
          baseNamespace = ((String) additionalProperties.get(BASE_NAMESPACE));
      }

      if (swagger.getInfo() != null) {
          Info info = swagger.getInfo();
          if (projectName == null &&  info.getTitle() != null) {
              // when projectName is not specified, generate it from info.title
              projectName = dashize(info.getTitle());
          }
          if (projectVersion == null) {
              // when projectVersion is not specified, use info.version
              projectVersion = info.getVersion();
          }
          if (projectDescription == null) {
              // when projectDescription is not specified, use info.description
              projectDescription = info.getDescription();
          }

          if (info.getContact() != null) {
              Contact contact = info.getContact();
              if (additionalProperties.get(PROJECT_URL) == null) {
                  additionalProperties.put(PROJECT_URL, contact.getUrl());
              }
          }
          if (info.getLicense() != null) {
              License license = info.getLicense();
              if (additionalProperties.get(PROJECT_LICENSE_NAME) == null) {
                  additionalProperties.put(PROJECT_LICENSE_NAME, license.getName());
              }
              if (additionalProperties.get(PROJECT_LICENSE_URL) == null) {
                  additionalProperties.put(PROJECT_LICENSE_URL, license.getUrl());
              }
          }
      }

      // default values
      if (projectName == null) {
          projectName = "swagger-cl-client";
      }
      if (projectVersion == null) {
          projectVersion = "1.0.0";
      }
      if (projectDescription == null) {
          projectDescription = "Client library of " + projectName;
      }
      if (baseNamespace == null) {
          baseNamespace = dashize(projectName);
      }
      apiPackage = baseNamespace + ".api";

      additionalProperties.put(PROJECT_NAME, projectName);
      additionalProperties.put(PROJECT_DESCRIPTION, escapeText(projectDescription));
      additionalProperties.put(PROJECT_VERSION, projectVersion);
      additionalProperties.put(BASE_NAMESPACE, baseNamespace);
      additionalProperties.put(CodegenConstants.API_PACKAGE, apiPackage);

      supportingFiles.add(new SupportingFile("asdf.mustache", "", projectName + ".asd"));
      supportingFiles.add(new SupportingFile("core.mustache", sourceFolder, "core.lisp"));
      supportingFiles.add(new SupportingFile("git_push.sh.mustache", "", "git_push.sh"));
      supportingFiles.add(new SupportingFile("gitignore.mustache", "", ".gitignore"));
  }

  /**
   * Escapes a reserved word as defined in the `reservedWords` array. Handle escaping
   * those terms here.  This logic is only called if a variable matches the reserved words
   * 
   * @return the escaped term
   */
  @Override
  public String escapeReservedWord(String name) {
    return "_" + name;  // add an underscore to the name
  }

  /**
   * Location to write model files.  You can use the modelPackage() as defined when the class is
   * instantiated
   */
  public String modelFileFolder() {
    return outputFolder + "/" + sourceFolder + "/" + modelPackage().replace('.', File.separatorChar);
  }

  /**
   * Optional - type declaration.  This is a String which is used by the templates to instantiate your
   * types.  There is typically special handling for different property types
   *
   * @return a string value used as the `dataType` field for model templates, `returnType` for api templates
   */
  @Override
  public String getTypeDeclaration(Property p) {
    if(p instanceof ArrayProperty) {
      ArrayProperty ap = (ArrayProperty) p;
      Property inner = ap.getItems();
      return getSwaggerType(p) + "[" + getTypeDeclaration(inner) + "]";
    }
    else if (p instanceof MapProperty) {
      MapProperty mp = (MapProperty) p;
      Property inner = mp.getAdditionalProperties();
      return getSwaggerType(p) + "[String, " + getTypeDeclaration(inner) + "]";
    }
    return super.getTypeDeclaration(p);
  }

  /**
   * Optional - swagger type conversion.  This is used to map swagger types in a `Property` into 
   * either language specific types via `typeMapping` or into complex models if there is not a mapping.
   *
   * @return a string value of the type or complex model for this property
   * @see io.swagger.models.properties.Property
   */
  @Override
  public String getSwaggerType(Property p) {
    String swaggerType = super.getSwaggerType(p);
    String type = null;
    if(typeMapping.containsKey(swaggerType)) {
      type = typeMapping.get(swaggerType);
      if(languageSpecificPrimitives.contains(type))
        return toModelName(type);
    }
    else
      type = swaggerType;
    return toModelName(type);
  }
  @SuppressWarnings("static-method")
  protected String namespaceToFolder(String ns) {
      return ns.replace(".", File.separator).replace("-", "_");
  }
  @Override
  public String sanitizeTag(String tag) {
      return tag.replaceAll("[^a-zA-Z_]+", "_");
  }

  @Override
  public String apiFileFolder() {
      return outputFolder + File.separator + sourceFolder + File.separator + "api";
  }

  @Override
  public String toOperationId(String operationId) {
      // throw exception if method name is empty
      if (StringUtils.isEmpty(operationId)) {
          throw new RuntimeException("Empty method/operation name (operationId) not allowed");
      }
      return dashize(sanitizeName(operationId));
  }

  @Override
  public String toApiFilename(String name) {
      return underscore(toApiName(name));
  }

  @Override
  public String toApiName(String name) {
      return dashize(name);
  }

  @Override
  public String toParamName(String name) {
      return toVarName(name);
  }

  @Override
  public String toVarName(String name) {
      name = name.replaceAll("[^a-zA-Z0-9_-]+", ""); // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.
      name = dashize(name);
      return name;
  }

  @Override
  public String escapeText(String input) {
      if (input == null) {
          return null;
      }
      return input.trim().replace("\\", "\\\\").replace("\"", "\\\"");
  }

  @Override
  public Map<String, Object> postProcessOperations(Map<String, Object> operations) {
      Map<String, Object> objs = (Map<String, Object>) operations.get("operations");
      List<CodegenOperation> ops = (List<CodegenOperation>) objs.get("operation");
      for (CodegenOperation op : ops) {
          // Convert httpMethod to lower case, e.g. "get", "post"
          op.httpMethod = op.httpMethod.toLowerCase();
      }
      return operations;
  }

  @Override
  public String escapeQuotationMark(String input) {
      // remove " to avoid code injection
      return input.replace("\"", "");
  }

  @Override
  public String escapeUnsafeCharacters(String input) {
      // ref: https://clojurebridge.github.io/community-docs/docs/clojure/comment/
      return input.replace("(comment", "(_comment");
  }
}
