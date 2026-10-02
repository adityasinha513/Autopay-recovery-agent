package com.razorpay.autopay.tool;

import com.razorpay.autopay.dto.CustomerDetailsResponse;
import com.razorpay.autopay.entity.Customer;
import com.razorpay.autopay.service.CustomerService;
import org.springframework.stereotype.Component;

@Component
public class CustomerTool {

    private final CustomerService customerService;

    public CustomerTool(CustomerService customerService) {
        this.customerService = customerService;
    }

    public CustomerDetailsResponse getCustomerDetails(Long customerId) {
        Customer customer = customerService.getCustomer(customerId);
        return new CustomerDetailsResponse(
                customer.getId(),
                customer.getName(),
                customer.getPhone(),
                customer.getAmountDue(),
                customer.getPaymentStatus(),
                customer.getFailureReason(),
                customer.getCreatedAt()
        );
    }
}
