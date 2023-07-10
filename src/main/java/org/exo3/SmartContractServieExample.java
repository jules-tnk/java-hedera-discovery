package org.exo3;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.hedera.hashgraph.sdk.*;

import io.github.cdimascio.dotenv.Dotenv;

public class SmartContractServieExample {
    public static void main(String[] args) {
        Client client = initializeClient();

        // AccountId otherAccountId = createAccount(client);

        showAccountInfo(client.getOperatorAccountId(), client);

        ContractId contractId = createContract(client);

        showContractInfo(contractId, client);

    }

    private static Client initializeClient() {
        AccountId myAccountId = AccountId.fromString(Dotenv.load().get("MY_ACCOUNT_ID"));
        PrivateKey myPrivateKey = PrivateKey.fromString(Dotenv.load().get("MY_PRIVATE_KEY"));

        Client client = Client.forTestnet();
        client.setOperator(myAccountId, myPrivateKey);
        client.setDefaultMaxTransactionFee(new Hbar(100));
        client.setDefaultMaxQueryPayment(new Hbar(50));
        System.out.println("Client initialized");
        return client;
    }

    private static AccountId createAccount(Client client) {
        System.out.println("Creating account...");
        try {
            PrivateKey newAccountPrivateKey = PrivateKey.generateED25519();
            PublicKey newAccountPublicKey = newAccountPrivateKey.getPublicKey();

            TransactionResponse newAccount = new AccountCreateTransaction()
                    .setKey(newAccountPublicKey)
                    .setInitialBalance(Hbar.fromTinybars(1000))
                    .execute(client);

            // Get the new account ID
            AccountId newAccountId = newAccount.getReceipt(client).accountId;

            System.out.println("New account ID: " + newAccountId);

            return newAccountId;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String getBytecodeHex(String filePath) {
        System.out.println("Getting bytecode from " + filePath);
        File file = new File(filePath);
        InputStream jsonStream = null;
        try {
            jsonStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        if (jsonStream == null) {
            throw new RuntimeException("failed to get " + filePath);
        }

        JsonObject json = new Gson()
                .fromJson(new InputStreamReader(jsonStream, StandardCharsets.UTF_8), JsonObject.class);

        if (json.has("object")) {
            return json.getAsJsonPrimitive("object").getAsString();
        }

        return json.getAsJsonPrimitive("bytecode").getAsString();
    }

    public static String getBytecode(String filePath) {
        System.out.println("Getting bytecode from " + filePath);
        ClassLoader cl = SmartContractServieExample.class.getClassLoader();

        Gson gson = new Gson();
        JsonObject jsonObject;

        // Get the json file
        InputStream jsonStream = cl.getResourceAsStream(filePath);
        jsonObject = gson.fromJson(new InputStreamReader(jsonStream, StandardCharsets.UTF_8), JsonObject.class);

        // Store the "object" field from the json file as hex-encoded bytecode
        String object = jsonObject.getAsJsonObject("data").getAsJsonObject("bytecode").get("object").getAsString();
        byte[] bytecode = object.getBytes(StandardCharsets.UTF_8);

        System.out.println("Bytecode retrieved");

        return new String(bytecode);
    }

    private static ContractId createContract(Client client) {
        try {
            String byteCodeHex;

            byteCodeHex = getBytecodeHex("HelloHedera.json");

            // Create the transaction
            System.out.println("Trying to create a contract...");
            ContractCreateFlow contractCreate = new ContractCreateFlow()
                    .setBytecode(byteCodeHex)
                    .setGas(100_000);

            TransactionResponse txResponse = contractCreate.execute(client);
            TransactionReceipt receipt = txResponse.getReceipt(client);

            // Get the new contract ID
            ContractId newContractId = receipt.contractId;

            System.out.println("The new contract ID is " + newContractId);

            return newContractId;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void showContractInfo(ContractId contractId, Client client) {
        System.out.println("Getting contract info from account ID: " + contractId);
        try {

            ContractInfo contractInfo = new ContractInfoQuery()
                    .setContractId(contractId)
                    .execute(client);
            System.out.println(contractInfo);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void showAccountInfo(AccountId accountId, Client client) {
        System.out.println("Getting account info from account ID: " + accountId);
        try {
            AccountInfo accountInfo = new AccountInfoQuery()
                    .setAccountId(accountId)
                    .execute(client);

            System.out.println(accountInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
