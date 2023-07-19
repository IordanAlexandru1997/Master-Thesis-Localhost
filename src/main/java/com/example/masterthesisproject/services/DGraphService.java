package com.example.masterthesisproject.services;

import com.example.masterthesisproject.entities.Person;
import com.google.protobuf.ByteString;
import io.dgraph.DgraphClient;
import io.dgraph.DgraphGrpc;
import io.dgraph.DgraphProto;
import io.dgraph.Transaction;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnExpression("#{T(com.example.masterthesisproject.services.DockerContainerChecker).isContainerRunning('dgraph')}")

public class DGraphService {
    private final DgraphClient dgraphClient;

    public DGraphService() {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9080).usePlaintext().build();
        DgraphGrpc.DgraphStub stub = DgraphGrpc.newStub(channel);
        this.dgraphClient = new DgraphClient(stub);

        // Set up index for "name" field
        String schema = "name: string @index(exact) .";
        DgraphProto.Operation op = DgraphProto.Operation.newBuilder().setSchema(schema).build();
        dgraphClient.alter(op);
    }

    public void insertPerson(Person person) {
        String mutationString = String.format(
                "_:x <name> \"%s\" .\n" +
                        "_:x <age> \"%d\" .\n",
                person.getName(), person.getAge());

        DgraphProto.Mutation mutation = DgraphProto.Mutation.newBuilder()
                .setSetNquads(ByteString.copyFromUtf8(mutationString))
                .build();

        Transaction txn = dgraphClient.newTransaction();
        try {
            txn.mutate(mutation);
            txn.commit();
        } finally {
            txn.discard();
        }
    }


    public String getPersonsByName(String name) {
        String query = String.format(
                "{\n" +
                        "  person(func: eq(name, \"%s\")) {\n" +
                        "    name\n" +
                        "    age\n" +
                        "  }\n" +
                        "}", name);

        DgraphProto.Response response = dgraphClient.newTransaction().query(query);
        return response.getJson().toStringUtf8();
    }
}
