package com.Springboot.Ticket_Booking_System.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "payments")
public class Payment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private Long bookingId;
	private Integer amount;
	private String paymentMethod;
	private String status;
	private String transactionId;
	@CreationTimestamp
	private LocalDateTime paidAt;

	public Payment() {
		super();
	}

	public Payment(Long id, Long bookingId, Integer amount, String paymentMethod, String status, String transactionId,
			LocalDateTime paidAt) {
		super();
		this.id = id;
		this.bookingId = bookingId;
		this.amount = amount;
		this.paymentMethod = paymentMethod;
		this.status = status;
		this.transactionId = transactionId;
		this.paidAt = paidAt;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getBookingId() {
		return bookingId;
	}

	public void setBookingId(Long bookingId) {
		this.bookingId = bookingId;
	}

	public Integer getAmount() {
		return amount;
	}

	public void setAmount(Integer amount) {
		this.amount = amount;
	}

	public String getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}

	public LocalDateTime getPaidAt() {
		return paidAt;
	}

	public void setPaidAt(LocalDateTime paidAt) {
		this.paidAt = paidAt;
	}

	@Override
	public String toString() {
		return "Payment [id=" + id + ", bookingId=" + bookingId + ", amount=" + amount + ", paymentMethod="
				+ paymentMethod + ", status=" + status + ", transactionId=" + transactionId + ", paidAt=" + paidAt
				+ "]";
	}

}
