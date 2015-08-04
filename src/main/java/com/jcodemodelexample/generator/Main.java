package com.jcodemodelexample.generator;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jboss.shrinkwrap.api.spec.JavaArchive;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JPrimitiveType;
import com.sun.codemodel.JStatement;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;

public class Main {

	private static JCodeModel cm = new JCodeModel();

	private static JPrimitiveType longType = JPrimitiveType.parse(cm, "long");

	public static void main(String[] args) {
		List<Class<?>> classes = ClassFinder.find("com.badion.dao");
		generateIntegrationTestClasses(classes);
	}

	private static void generateIntegrationTestClasses(List<Class<?>> classes) {
		for (int i = 0; i < classes.size(); i++) {
			JDefinedClass dc = createClass(classes.get(i));
			JFieldVar fieldForInject = createFieldForInjection(classes.get(i), dc);
			createDeploymentMethod(dc);
			addTest(dc, classes.get(i), fieldForInject);
			createGetAllMethod(dc, fieldForInject);
			createFindByMethod(dc, fieldForInject);
			deleteByObject(dc, fieldForInject);
			createFile("D:/newWorkspace/ecclLastIntegration/eccl-ejb/src/test/java");
		}
	}

	private static void createGetAllMethod(JDefinedClass dc, JFieldVar fieldForInject) {
		JMethod addGetAllMethod = dc.method(JMod.PUBLIC, cm.VOID, "getAllTest");
		addGetAllMethod.annotate(cm.ref("org.junit.Test"));
		JExpression getAllMethod = fieldForInject.invoke("getAll");
		JInvocation assertNotNull = cm.directClass("org.junit.Assert").staticInvoke("assertNotNull").arg(getAllMethod);
		addGetAllMethod.body().add(assertNotNull);
	}

	private static void createFindByMethod(JDefinedClass dc, JFieldVar fieldForInject) {
		JMethod addFindByIdMethod = dc.method(JMod.PUBLIC, cm.VOID, "findById");
		addFindByIdMethod.annotate(cm.ref("org.junit.Test"));
		JExpression findByIdMethod = fieldForInject.invoke("findById").arg(JExpr.lit((long) 10000L)).invoke("getId");
		JInvocation assertEquals = cm.directClass("org.junit.Assert").staticInvoke("assertEquals").arg(JExpr.cast(longType, findByIdMethod))
				.arg(JExpr.cast(longType, JExpr.lit(10000L)));
		addFindByIdMethod.body().add(assertEquals);
	}

	private static void deleteByObject(JDefinedClass dc, JFieldVar fieldForInject) {
		JMethod deleteByObject = dc.method(JMod.PUBLIC, cm.VOID, "deleteByObject");
		deleteByObject.annotate(cm.ref("org.junit.Test"));
		JExpression findByIdMethod = fieldForInject.invoke("findById").arg(JExpr.lit((long) 10000L));
		JInvocation assertNotNull = cm.directClass("org.junit.Assert").staticInvoke("assertNotNull").arg(findByIdMethod);
		JExpression deleteByIdMethod = fieldForInject.invoke("delete").arg(findByIdMethod);
		JInvocation assertNull = cm.directClass("org.junit.Assert").staticInvoke("assertNull").arg(deleteByIdMethod);
		deleteByObject.body().add(assertNotNull);
		deleteByObject.body().add(assertNull);
	}

	private static void addTest(JDefinedClass dc, Class<?> clazz, JFieldVar daoField) {
		if (clazz.getName().contains("Generic")) {
			return;
		}

		JMethod addTestMethod = dc.method(JMod.PUBLIC, cm.VOID, "addTest");
		addTestMethod.annotate(cm.ref("org.junit.Test"));

		Class<?> modelClass;
		try {
			modelClass = Class.forName(clazz.getName().replace("dao", "model").replace("Dao", ""));
			JType modelJType = cm._ref(modelClass);
			JVar var = addTestMethod.body().decl(modelJType, modelClass.getSimpleName().toLowerCase(), JExpr._new(modelJType));
			JExpression getId = var.invoke("getId");
			JExpression addEntity = daoField.invoke("add").arg(var);

			JInvocation assertEqualsInvoke = cm.directClass("org.junit.Assert").staticInvoke("assertEquals").arg(JExpr.cast(longType, JExpr.lit(10000L))).arg(JExpr.cast(longType,getId));

			JExpression setIdExpression = var.invoke("setId").arg(JExpr.lit(10000L));

			addTestMethod.body().add((JStatement) setIdExpression);
			addTestMethod.body().add((JStatement) addEntity);
			addTestMethod.body().add(assertEqualsInvoke);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

	}

	private static JDefinedClass createClass(Class<?> clazz) {
		JDefinedClass dc = null;
		try {
			dc = cm._class(clazz.getCanonicalName() + "Test");
			dc.annotate(cm.ref("org.junit.runner.RunWith")).param("value", "org.jboss.arquillian.junit.Arquillian.class");
		} catch (JClassAlreadyExistsException e) {
			e.printStackTrace();
		}
		return dc;
	}

	private static JFieldVar createFieldForInjection(Class<?> clazz, JDefinedClass dc) {
		JFieldVar fieldForInject = dc.field(JMod.PRIVATE, clazz, clazz.getSimpleName().toLowerCase());
		fieldForInject.annotate(cm.ref("javax.inject.Inject"));
		return fieldForInject;
	}

	private static void createDeploymentMethod(JDefinedClass dc) {
		JMethod m = dc.method(JMod.PUBLIC | JMod.STATIC, JavaArchive.class, "createDeployment");
		m.annotate(cm.ref("org.jboss.arquillian.container.test.api.Deployment"));
		m.annotate(cm.ref("org.jboss.arquillian.container.test.api.OverProtocol")).param("value", "Servlet 3.0");
		JBlock jblock = m.body();
		JClass webIntegrationClass = cm.ref("com.badion.util.WebIntegrationTemplate");
		jblock.decl(webIntegrationClass, "template").init(webIntegrationClass.staticInvoke("newInstance").arg(dc.dotclass()));
		m.body()._return(webIntegrationClass.staticInvoke("build"));
	}

	private static void createFile(String path) {
		File file = new File(path);
		file.mkdirs();
		try {
			cm.build(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
