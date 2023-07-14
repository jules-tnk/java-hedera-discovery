package org.exo3;


import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import com.hedera.hashgraph.sdk.*;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SmartContractServiceExample {

public static void main(String[] args) throws Exception {

//Grab your Hedera Testnet account ID and private key
AccountId myAccountId = AccountId.fromString(Dotenv.load().get("MY_ACCOUNT_ID"));
PrivateKey myPrivateKey = PrivateKey.fromString(Dotenv.load().get("MY_PRIVATE_KEY"));

AccountId AId = AccountId.fromString(Dotenv.load().get("A_ACCOUNT_ID"));
PrivateKey APrivateKey = PrivateKey.fromString(Dotenv.load().get("A_PRIVATE_KEY"));


//Create your Hedera Testnet client
Client client = Client.forTestnet();
Client A = Client.forTestnet();

//Set your account as the client's operator
client.setOperator(myAccountId, myPrivateKey);
A.setOperator(AId, APrivateKey);

//Set the default maximum transaction fee (in Hbar)
client.setDefaultMaxTransactionFee(new Hbar(100));
A.setDefaultMaxTransactionFee(new Hbar(100));

//Set the maximum payment for queries (in Hbar)
client.setDefaultMaxQueryPayment(new Hbar(50));
A.setDefaultMaxQueryPayment(new Hbar(50));




//Import the compiled contract from the HelloHedera.json file
Gson gson = new Gson();
JsonObject jsonObject;

InputStream jsonStream = SmartContractService.class.getClassLoader().getResourceAsStream("HelloHedera.json");
jsonObject = gson.fromJson(new InputStreamReader(jsonStream, StandardCharsets.UTF_8), JsonObject.class);

//Store the "object" field from the HelloHedera.json file as hex-encoded bytecode
String object = jsonObject.getAsJsonPrimitive("bytecode").getAsString();
byte[] bytecode = object.getBytes(StandardCharsets.UTF_8);

//Create a file on Hedera and store the hex-encoded bytecode
FileCreateTransaction fileCreateTx = new FileCreateTransaction()
//Set the bytecode of the contract
.setContents(bytecode);

//Submit the file to the Hedera test network signing with the transaction fee payer key specified with the client
TransactionResponse submitTx = fileCreateTx.execute(client);

//Get the receipt of the file create transaction
TransactionReceipt fileReceipt = submitTx.getReceipt(client);

//Get the file ID from the receipt
FileId bytecodeFileId = fileReceipt.fileId;

//Log the file ID
System.out.println("The smart contract bytecode file ID is " +bytecodeFileId);


AccountId addressToPass = AccountId.fromString("0.0.1465703");
// Instantiate the contract instance
ContractCreateTransaction contractTx = new ContractCreateTransaction()
//Set the file ID of the Hedera file storing the bytecode
.setBytecodeFileId(bytecodeFileId)
//Set the gas to instantiate the contract
.setGas(100_000)
//Provide the constructor parameters for the contract
.setConstructorParameters(new ContractFunctionParameters()
//The account ID to associate the token to
.addAddress(addressToPass.toSolidityAddress()));

//Submit the transaction to the Hedera test network
TransactionResponse contractResponse = contractTx.execute(client);

//Get the receipt of the file create transaction
TransactionReceipt contractReceipt = contractResponse.getReceipt(client);


//Get the smart contract ID
ContractId newContractId = contractReceipt.contractId;


//Log the smart contract ID
System.out.println("The smart contract ID is " + newContractId);




//get_adress
//Create the transaction
ContractExecuteTransaction transaction1 = new ContractExecuteTransaction()
.setContractId(newContractId)
.setGas(100_000)
.setFunction("get_address");
//Sign with the client operator private key to pay for the transaction and submit the query to a Hedera network
TransactionResponse txResponse1 = transaction1.execute(client);
//Request the receipt of the transaction
TransactionReceipt receipt1 = txResponse1.getReceipt(client);

//Get the transaction consensus status
Status transactionStatus1 = receipt1.status;

System.out.println("The transaction consensus status of get_adress is " +transactionStatus1);
System.out.println("le recu de la transaction get_adress: "+ receipt1);



// Créer la transaction d'appel de contrat pour la fonction get_address
ContractCallQuery callQuery = new ContractCallQuery()
.setContractId(newContractId) // Utilisez l'ID du contrat que vous avez créé précédemment
.setGas(100_000)
.setFunction("get_address");

// Exécuter la requête d'appel de contrat sur le réseau Hedera en utilisant le client
ContractFunctionResult callResult = callQuery.execute(client);

// Récupérer le résultat de la fonction get_address
String address = String.valueOf(callResult);

// Afficher l'adresse de votre compte testnet
System.out.println("L'adresse de votre compte testnet est : " + address);





//set_adress

AccountId accountId = AccountId.fromString(String.valueOf(AId));
ContractExecuteTransaction transaction33 = new ContractExecuteTransaction()
.setContractId(newContractId)
.setGas(100_000)
.setFunction("set_address", new ContractFunctionParameters()
//The account ID to associate the token to
.addAddress(accountId.toSolidityAddress()));


TransactionResponse txResponse33 = transaction33.execute(client);
//Request the receipt of the transaction
TransactionReceipt receipt33 = txResponse33.getReceipt(client);

//Get the transaction consensus status
Status transactionStatus33 = receipt33.status;

System.out.println("The transaction consensus status of set_adress is " +transactionStatus33);
System.out.println("le recu de la transaction set_adress: "+ receipt33);


//get_adress
//Create the transaction
ContractExecuteTransaction transaction3 = new ContractExecuteTransaction()
.setContractId(newContractId)
.setGas(100_000)
.setFunction("get_address");
//Sign with the client operator private key to pay for the transaction and submit the query to a Hedera network
TransactionResponse txResponse3 = transaction3.execute(A);
//Request the receipt of the transaction
TransactionReceipt receipt3 = txResponse3.getReceipt(A);

//Get the transaction consensus status
Status transactionStatus3 = receipt3.status;




System.out.println("The transaction consensus status of get_adress is " +transactionStatus3);
System.out.println("le recu de la transaction get_adress: "+ receipt3);

// Créer la transaction d'appel de contrat pour la fonction get_address
ContractCallQuery callQuery2 = new ContractCallQuery()
.setContractId(newContractId) // Utilisez l'ID du contrat que vous avez créé précédemment
.setGas(100_000)
.setFunction("get_address");

// Exécuter la requête d'appel de contrat sur le réseau Hedera en utilisant le client
ContractFunctionResult callResult2 = callQuery2.execute(A);

// Récupérer le résultat de la fonction get_address
ContractFunctionResult address2 = callResult2;

// Afficher l'adresse de votre compte testnet
System.out.println("L'adresse de votre compte testnet est (changé) : " + address2);






}
}