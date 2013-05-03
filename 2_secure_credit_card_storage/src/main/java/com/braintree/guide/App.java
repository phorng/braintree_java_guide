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
import com.braintreegateway.Customer;
import com.braintreegateway.CustomerRequest;
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

        post(new Route("/create_customer") {
            @Override
            public Object handle(Request request, Response response) {
                CustomerRequest customerRequest = new CustomerRequest()
                    .firstName(request.queryParams("first_name"))
                    .lastName(request.queryParams("last_name"))
                    .creditCard()
                        .billingAddress()
                            .postalCode(request.queryParams("postal_code"))
                            .done()
                        .number(request.queryParams("number"))
                        .expirationMonth(request.queryParams("month"))
                        .expirationYear(request.queryParams("year"))
                        .cvv(request.queryParams("cvv"))
                        .done();

                Result<Customer> result = gateway.customer().create(customerRequest);

                response.type("text/html");
                if (result.isSuccess()) {
                  return "<h2>Customer created with name: " + result.getTarget().getFirstName() + " " + result.getTarget().getLastName() + "</h2>";
                } else {

                  return "<h2>Error: " + result.getMessage() + "</h2>";
                }
            }
        });
    }
}
