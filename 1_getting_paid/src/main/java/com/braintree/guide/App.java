package com.braintree.guide;

import static spark.Spark.get;
import static spark.Spark.post;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;

import org.apache.commons.io.FileUtils;

import spark.Request;
import spark.Response;
import spark.Route;

import com.braintreegateway.BraintreeGateway;
import com.braintreegateway.Environment;
import com.braintreegateway.Result;
import com.braintreegateway.Transaction;
import com.braintreegateway.TransactionRequest;

public class App {
    private static BraintreeGateway gateway = new BraintreeGateway(
        Environment.SANDBOX,
        "use_your_merchant_id",
        "use_your_public_key",
        "use_your_private_key"
    );

    private static String renderHtml(String pageName) {
        try {
            return FileUtils.readFileToString(new File(pageName));
        } catch (IOException e) {
            return "Couldn't find " + pageName;
        }
    }

    public static void main(String[] args) {
        get(new Route("/") {
            @Override
            public Object handle(Request request, Response response) {
                response.type("text/html");
                return renderHtml("views/braintree.html");
            }
        });

        post(new Route("/create_transaction") {
            @Override
            public Object handle(Request request, Response response) {
                TransactionRequest transactionRequest = new TransactionRequest()
                    .amount(new BigDecimal("1000.00"))
                    .creditCard()
                        .number(request.queryParams("number"))
                        .cvv(request.queryParams("cvv"))
                        .expirationDate(request.queryParams("expiration_date"))
                        .done()
                    .options()
                        .submitForSettlement(true)
                        .done();

                Result<Transaction> result = gateway.transaction().sale(transactionRequest);

                response.type("text/html");
                if (result.isSuccess()) {
                  return "<h1>Success! Transaction ID: " + result.getTarget().getId() + "</h1>";
                } else {
                  return "<h1>Error: " + result.getMessage() + "</h1>";
                }
            }
        });
    }
}
