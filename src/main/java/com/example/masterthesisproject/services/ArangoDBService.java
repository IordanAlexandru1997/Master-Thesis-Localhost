package com.example.masterthesisproject.services;
import com.arangodb.entity.CollectionEntity;
import com.arangodb.model.CollectionCreateOptions;
import com.arangodb.entity.CollectionType;

import com.arangodb.ArangoDB;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.BaseDocument;
import com.example.masterthesisproject.entities.Employee;
import com.example.masterthesisproject.entities.Invoice;
import com.example.masterthesisproject.entities.Project;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

@Service
public class ArangoDBService {

    private static final String ARANGO_DB_HOST = "localhost";
    private static final int ARANGO_DB_PORT = 8529;
    private static final String ARANGO_DB_USER = "root";
    private static final String ARANGO_DB_PASSWORD = "password";
    private static final String DB_NAME = "arangodb";

    private ArangoDB arangoDB;
    private ArangoDatabase database;

    @PostConstruct
    public void init() {
        arangoDB = new ArangoDB.Builder()
                .host(ARANGO_DB_HOST, ARANGO_DB_PORT)
                .user(ARANGO_DB_USER)
                .password(ARANGO_DB_PASSWORD)
                .build();
        database = arangoDB.db(DB_NAME);
    }

    public void createRelationships(String employeeName, String projectName) {
        String employeeKey;
        Employee employee = getEmployee(employeeName);
        if (employee != null) {
            employeeKey = employee.getId();
        } else {
            throw new RuntimeException("Employee not found: " + employeeName);
        }

        String projectKey;
        Project project = getProjectByName(projectName);
        if (project != null) {
            projectKey = project.getId();
        } else {
            throw new RuntimeException("Project not found: " + projectName);
        }

        // Create the Edge collection if it doesn't exist
        if (!database.collection("WorksOn").exists()) {
            CollectionCreateOptions options = new CollectionCreateOptions();
            options.type(CollectionType.EDGES);
            CollectionEntity collection = database.createCollection("WorksOn", options);
        }

        // Define an edge document
        BaseDocument worksOnEdge = new BaseDocument();
        String edgeKey = employeeKey + "_works_on_" + projectKey;
        if (relationshipExists(employeeKey, projectKey)) {
            worksOnEdge.setKey(edgeKey); // unique key for this relationship
            worksOnEdge.addAttribute("_from", "Employee/" + employeeKey); // the '_from' attribute points to the employee
            worksOnEdge.addAttribute("_to", "Project/" + projectKey); // the '_to' attribute points to the project

            // Save the edge document in the "WorksOn" collection
            database.collection("WorksOn").insertDocument(worksOnEdge);
        } else {
            throw new RuntimeException("Relationship already exists: " + edgeKey);
        }
    }


    public Project getProjectByName(String name) {
        String query = "FOR p IN Project FILTER p.name == @name RETURN p";
        Map<String, Object> bindVars = new HashMap<>();
        bindVars.put("name", name);
        return database.query(query, bindVars, null, Project.class).first();
    }

    public void createRelationshipWithoutInvoice(String employeeName, String projectName) {
        BaseDocument employee = new BaseDocument();
        employee.addAttribute("name", employeeName);
        database.collection("Employee").insertDocument(employee);

        BaseDocument project = new BaseDocument();
        project.addAttribute("name", projectName);
        database.collection("Project").insertDocument(project);

          }

    public Employee getEmployee(String name) {
        String query = "FOR e IN Employee FILTER e.name == @name RETURN e";
        Map<String, Object> bindVars = new HashMap<>();
        bindVars.put("name", name);
        return database.query(query, bindVars, null, Employee.class).first();
    }
    public void createEmployee(Employee employee) {
        BaseDocument baseEmployee = new BaseDocument(employee.getName());
        baseEmployee.addAttribute("name", employee.getName());
        baseEmployee.addAttribute("salary", employee.getSalary());
        baseEmployee.addAttribute("department", employee.getDepartment());
        database.collection("Employee").insertDocument(baseEmployee);
    }
    public void createInvoice(Invoice invoice) {
        BaseDocument baseInvoice = new BaseDocument(invoice.getId());
        baseInvoice.addAttribute("customer", invoice.getCustomer());
        baseInvoice.addAttribute("amount", invoice.getAmount());
        database.collection("Invoice").insertDocument(baseInvoice);
    }

    public void createProject(Project project) {
        BaseDocument baseProject = new BaseDocument(project.getName());
        baseProject.addAttribute("name", project.getName());
        database.collection("Project").insertDocument(baseProject);
    }
    public Invoice getInvoice(String id) {
        String query = "FOR i IN Invoice FILTER i._key == @id RETURN i";
        Map<String, Object> bindVars = new HashMap<>();
        bindVars.put("id", id);
        return database.query(query, bindVars, null, Invoice.class).first();
    }
    public Project getProject(String id) {
        String query = "FOR p IN Project FILTER p._key == @id RETURN p";
        Map<String, Object> bindVars = new HashMap<>();
        bindVars.put("id", id);
        return database.query(query, bindVars, null, Project.class).first();
    }

    public List<Employee> getEmployeesByName(String name) {
        String query = "FOR e IN Employee FILTER e.name == @name RETURN e";
        Map<String, Object> bindVars = new HashMap<>();
        bindVars.put("name", name);
        return database.query(query, bindVars, null, Employee.class).asListRemaining();
    }


    public void createInvoiceAndRelationship(String employeeName, Invoice invoice) {
        String employeeKey;
        Employee employee = getEmployee(employeeName);
        if (employee != null) {
            employeeKey = employee.getId();
        } else {
            throw new RuntimeException("Employee not found: " + employeeName);
        }

        // Create the Invoice document
        BaseDocument baseInvoice = new BaseDocument(invoice.getId());
        baseInvoice.addAttribute("customer", invoice.getCustomer());
        baseInvoice.addAttribute("amount", invoice.getAmount());
        database.collection("Invoice").insertDocument(baseInvoice);

        // Create the Edge collection if it doesn't exist
        if (!database.collection("IssueInvoice").exists()) {
            CollectionCreateOptions options = new CollectionCreateOptions();
            options.type(CollectionType.EDGES);
            CollectionEntity collection = database.createCollection("IssueInvoice", options);
        }

        // Define an edge document
        BaseDocument issueInvoiceEdge = new BaseDocument();
        String edgeKey = employeeKey + "_issues_invoice_" + invoice.getId();
        if (relationshipExists(employeeKey, invoice.getId())) {
            issueInvoiceEdge.setKey(edgeKey); // unique key for this relationship
            issueInvoiceEdge.addAttribute("_from", "Employee/" + employeeKey); // the '_from' attribute points to the employee
            issueInvoiceEdge.addAttribute("_to", "Invoice/" + invoice.getId()); // the '_to' attribute points to the invoice

            // Save the edge document in the "IssueInvoice" collection
            database.collection("IssueInvoice").insertDocument(issueInvoiceEdge);
        } else {
            throw new RuntimeException("Relationship already exists: " + edgeKey);
        }
    }

    // Add a new method to check if a relationship already exists
    public boolean relationshipExists(String employeeKey, String invoiceId) {
        String query = "FOR i IN IssueInvoice FILTER i._key == @key RETURN i";
        Map<String, Object> bindVars = new HashMap<>();
        bindVars.put("key", employeeKey + "_issues_invoice_" + invoiceId);
        return database.query(query, bindVars, null, BaseDocument.class).first() == null;
    }
}
