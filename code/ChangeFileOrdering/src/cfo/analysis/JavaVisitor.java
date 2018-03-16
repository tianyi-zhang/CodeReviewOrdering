package cfo.analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class JavaVisitor extends ASTVisitor{
	public HashMap<String, ArrayList<String>> call_relation;
	public ArrayList<String> interfaces;
	public String superClass;
	public String curClass;
	public boolean isInterface;
	
	public JavaVisitor() {
		this.call_relation = new HashMap<String, ArrayList<String>>();
		this.interfaces = new ArrayList<String>();
		this.superClass = null;
		this.curClass = null;
	}
	
	@Override
	public boolean visit(TypeDeclaration node){
		String name = node.getName().toString();
		if(curClass != null) {
			// ignore other inner classes
			return false;
		} else {
			curClass = name;
			Type superType = node.getSuperclassType();
			if(superType != null) {
				superClass = superType.toString();
			}
			
			isInterface = node.isInterface();
			List<Type> itf = node.superInterfaceTypes();
			for(Type t : itf) {
				interfaces.add(t.toString());
			}
			return true;
		}
	}
	
	private String curMethod = null;
	
	@Override
	public boolean visit(MethodDeclaration node){
		String mName = node.getName().toString();
		curMethod = mName;
		call_relation.put(curMethod, new ArrayList<String>());
		return true;
	}
	
	@Override
	public void endVisit(MethodInvocation node) {
		curMethod = null;
	}
	
	@Override
	public boolean visit(MethodInvocation node) {
		String name = node.getName().toString();
		if(curMethod != null) {
			ArrayList<String> call = call_relation.get(curMethod);
			call.add(name);
			call_relation.put(curMethod, call);
		}
		
		return true;
	}
	
	@Override
	public boolean visit(ClassInstanceCreation node) {
		String name = node.getType().toString();
		if(curMethod != null) {
			ArrayList<String> call = call_relation.get(curMethod);
			call.add("new " + name);
			call_relation.put(curMethod, call);
		}
		
		return true;
	}
	
	public static void main(String[] args) throws IOException {
		String file = "src/cfo/analysis/JavaParseUtils.java";
		CompilationUnit cu = JavaParseUtils.parse(file);
		JavaVisitor jv = new JavaVisitor();
		cu.accept(jv);
		
		// print out the class info in this java file
		if(jv.isInterface) {
			System.out.println("Interface Name: " + jv.curClass);
			System.out.println("Extended Interfaces: " + jv.interfaces);
			
			// there are no method calls in an interface
			// simply print all declared methods
			System.out.println("Declared Methods: " + jv.call_relation.keySet());
		} else {
			System.out.println("Class Name: " + jv.curClass);
			System.out.println("Extended Class: " + jv.superClass);
			System.out.println("Implemented Interfaces: " + jv.interfaces);
			
			// print the declared methods and the other methods called inside each method
			for(String m : jv.call_relation.keySet()) {
				System.out.println("Declared Method: " + m);
				System.out.println("Called Methods in " + m + ":" + jv.call_relation.get(m));
			}
		}
	}
}
