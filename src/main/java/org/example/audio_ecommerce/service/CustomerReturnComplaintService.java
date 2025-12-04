package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.request.CustomerReturnComplaintCreateRequest;

public interface CustomerReturnComplaintService {

    void createComplaint(CustomerReturnComplaintCreateRequest req);

    void processAutoRefundComplaints();
}
