package com.example.masterthesisproject.services;

import com.example.masterthesisproject.entities.Employee;
import com.example.masterthesisproject.entities.Invoice;
import com.example.masterthesisproject.entities.Person;
import com.example.masterthesisproject.entities.Project;
import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.MessageSerializer;
import org.apache.tinkerpop.gremlin.driver.Result;
import org.apache.tinkerpop.gremlin.driver.ser.GryoMessageSerializerV3d0;
import org.apache.tinkerpop.gremlin.driver.ser.Serializers;
import org.apache.tinkerpop.gremlin.structure.io.gryo.GryoMapper;
import org.janusgraph.graphdb.tinkerpop.JanusGraphIoRegistry;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Service
public class JanusGraphService {
    private Client client;

    public JanusGraphService() {
        GryoMapper.Builder builder = GryoMapper.build().addRegistry(JanusGraphIoRegistry.getInstance());


        Cluster cluster = Cluster.build()
                .addContactPoint("localhost")
                .port(8182)
                .serializer(Serializers.GRAPHBINARY_V1D0)
                .credentials("janusgraph", "parola")
                .create();

        this.client = cluster.connect();
    }
    public void insertEmployee(Employee employee) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", employee.getName());
        params.put("salary", employee.getSalary());
        params.put("empId", employee.getId());
        params.put("department", employee.getDepartment());
        client.submit("g.addV('employee').property('name', name).property('salary', salary).property('empId', empId).property('department', department)", params).all().join();
    }

    public void insertProject(Project project) {
        Map<String, Object> params = new HashMap<>();
        params.put("projectId", project.getId());
        params.put("name", project.getName());
        client.submit("g.addV('project').property('projectId', projectId).property('name', name)", params).all().join();
    }

    public void insertInvoice(Invoice invoice) {
        Map<String, Object> params = new HashMap<>();
        params.put("invoiceId", invoice.getId());
        params.put("customer", invoice.getCustomer());
        params.put("amount", invoice.getAmount());
        client.submit("g.addV('invoice').property('invoiceId', invoiceId).property('customer', customer).property('amount', amount)", params).all().join();
    }

    public void addRelationshipEmployeeInvoice(String employeeId, String invoiceId) {
        Map<String, Object> params = new HashMap<>();
        params.put("employeeId", employeeId);
        params.put("invoiceId", invoiceId);
        client.submit("g.V().has('employee', 'empId', employeeId).as('a').V().has('invoice', 'invoiceId', invoiceId).addE('issues').from('a')", params).all().join();
    }

    public void addRelationshipEmployeeProject(String employeeId, String projectId) {
        Map<String, Object> params = new HashMap<>();
        params.put("employeeId", employeeId);
        params.put("projectId", projectId);
        client.submit("g.V().has('employee', 'empId', employeeId).as('a').V().has('project', 'projectId', projectId).addE('worksOn').from('a')", params).all().join();
    }
}


