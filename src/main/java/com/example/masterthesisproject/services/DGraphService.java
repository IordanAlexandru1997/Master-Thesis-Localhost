package com.example.masterthesisproject.services;

import com.example.masterthesisproject.entities.Employee;
import com.example.masterthesisproject.entities.Invoice;
import com.example.masterthesisproject.entities.Project;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import io.dgraph.DgraphClient;
import io.dgraph.DgraphGrpc;
import io.dgraph.DgraphProto;
import io.dgraph.Transaction;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@ConditionalOnExpression("#{T(com.example.masterthesisproject.services.DockerContainerChecker).isContainerRunning('dgraph')}")

public class DGraphService {

    private DgraphClient dgraphClient;

    public DGraphService() {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9081).usePlaintext().build();
        DgraphGrpc.DgraphStub stub = DgraphGrpc.newStub(channel);
        this.dgraphClient = new DgraphClient(stub);
        createSchema();
    }

    public void createSchema() {
        try {
            String schema = "name: string @index(exact) .\n"
                    + "salary: float .\n"
                    + "department: string @index(exact) .\n"
                    + "invoiceId: string @index(exact) .\n"
                    + "amount: float .\n"
                    + "customer: string @index(exact) .\n"
                    + "projectName: string @index(exact) .\n"
                    + "worksOn: [uid] @reverse .\n"
                    + "issues: [uid] @reverse .\n"
                    + "type Employee {\n name\n salary\n department\n worksOn\n issues\n}\n"
                    + "type Invoice {\n invoiceId\n amount\n customer\n}\n"
                    + "type Project {\n projectName\n }";
            DgraphProto.Operation op = DgraphProto.Operation.newBuilder().setSchema(schema).build();

            dgraphClient.alter(op);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public String buildRdfFromEmployee(Employee employee) {
        String employeeUid = "_:employee";
        return String.format(Locale.US,
                "<%s> <name> \"%s\" .\n<%s> <salary> \"%f\" .\n<%s> <department> \"%s\" .\n<%s> <dgraph.type> \"Employee\" .",
                employeeUid, employee.getName(),
                employeeUid, employee.getSalary(),
                employeeUid, employee.getDepartment(),
                employeeUid);
    }

    public void createEmployee(Employee employee) {
        String employeeNode = buildRdfFromEmployee(employee);
        insertData(employeeNode);
    }

    public void createProject(Project project) {
        String projectNode = buildRdfFromProject(project);
        insertData(projectNode);
    }

    public Employee getEmployeeByName(String name) {
        String query = String.format("query {\n employee(func: eq(name, \"%s\")) {\n uid\n name\n salary\n department\n }\n}", name);
        DgraphProto.Response response = dgraphClient.newReadOnlyTransaction().query(query);

        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(response.getJson().toStringUtf8());
            JsonNode employeeNode = root.get("employee").get(0);
            if (employeeNode != null) {
                String uid = employeeNode.get("uid").asText();
                String employeeName = employeeNode.get("name").asText();
                float salary = employeeNode.get("salary").floatValue();
                String department = employeeNode.get("department").asText();

                Employee employee = new Employee();
                employee.setName(employeeName);
                employee.setSalary(salary);
                employee.setDepartment(department);

                return employee;
            } else {
                throw new Exception("No employee found with name: " + name);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public String buildRdfFromInvoice(Invoice invoice) {
        String invoiceUid = "_:invoice";
        return String.format(Locale.US,
                "<%s> <invoiceId> \"%s\" .\n<%s> <amount> \"%f\" .\n<%s> <dgraph.type> \"Invoice\" .",
                invoiceUid, invoice.getId(),
                invoiceUid, invoice.getAmount(),
                invoiceUid);
    }

    public void createInvoice(Invoice invoice) {
        String invoiceNode = buildRdfFromInvoice(invoice);
        insertData(invoiceNode);
    }

    public void createEmployeeProjectRelationship(String employeeName, String projectName) {
        String employeeUid = getEmployeeUidByName(employeeName);
        String projectUid = getProjectUidByName(projectName);
        if(employeeUid != null && projectUid != null) {
            String relData = String.format("<%s> <worksOn> <%s> .", employeeUid, projectUid);
            insertData(relData);
        } else {
            throw new RuntimeException("Either employee or project does not exist");
        }
    }
    public String getProjectUidByName(String name) {
        String query = String.format("query {\n project(func: eq(projectName, \"%s\")) {\n uid\n }\n}", name);
        DgraphProto.Response response = dgraphClient.newReadOnlyTransaction().query(query);

        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(response.getJson().toStringUtf8());
            JsonNode projectNode = root.get("project").get(0);
            if (projectNode != null) {
                return projectNode.get("uid").asText();
            } else {
                throw new Exception("No project found with name: " + name);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String buildRdfFromProject(Project project) {
        String projectUid = "_:project";
        return String.format(Locale.US,
                "<%s> <projectName> \"%s\" .\n<%s> <dgraph.type> \"Project\" .",
                projectUid, project.getName(),
                projectUid);
    }




    public void createEmployeeInvoiceRelationship(String employeeName, String invoiceId) {
        String employeeUid = getEmployeeUidByName(employeeName);
        String invoiceUid = getInvoiceUidById(invoiceId);
        if(employeeUid != null && invoiceUid != null) {
            String relData = String.format("<%s> <issues> <%s> .", employeeUid, invoiceUid);
            insertData(relData);
        } else {
            throw new RuntimeException("Either employee or invoice does not exist");
        }
    }


    private void insertData(String data) {
        Transaction txn = dgraphClient.newTransaction();
        try {
            DgraphProto.Mutation mutation = DgraphProto.Mutation.newBuilder().setSetNquads(ByteString.copyFrom(data.getBytes())).build();
            txn.mutate(mutation);
            txn.commit();
        } finally {
            txn.discard();
        }
    }




    public String getEmployeeUidByName(String name) {
        String query = String.format("query {\n employee(func: eq(name, \"%s\")) {\n uid\n }\n}", name);
        DgraphProto.Response response = dgraphClient.newReadOnlyTransaction().query(query);

        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(response.getJson().toStringUtf8());
            JsonNode employeeNode = root.get("employee").get(0);
            if (employeeNode != null) {
                return employeeNode.get("uid").asText();
            } else {
                throw new Exception("No employee found with name: " + name);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }



    public String getInvoiceUidById(String id) {
        String query = String.format("query {\n invoice(func: eq(invoiceId, \"%s\")) {\n uid\n }\n}", id);
        DgraphProto.Response response = dgraphClient.newReadOnlyTransaction().query(query);

        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(response.getJson().toStringUtf8());
            JsonNode invoiceNode = root.get("invoice").get(0);
            if (invoiceNode != null) {
                return invoiceNode.get("uid").asText();
            } else {
                throw new Exception("No invoice found with id: " + id);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


}
