package com.Springboot.Ticket_Booking_System.dto;

public class UserDTO {
	private Long id;
	private String name;
	private String email;
	private String role;
	private String clerkUserId;

	public UserDTO(Long id, String name, String email, String role, String clerkUserId) {
		this.id = id;
		this.name = name;
		this.email = email;
		this.role = role;
		this.clerkUserId = clerkUserId;
	}

	public UserDTO() {
		super();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getClerkUserId() {
		return clerkUserId;
	}

	public void setClerkUserId(String clerkUserId) {
		this.clerkUserId = clerkUserId;
	}

	@Override
	public String toString() {
		return "UserDTO [id=" + id + ", name=" + name + ", email=" + email + ", role=" + role
				+ ", clerkUserId=" + clerkUserId + "]";
	}
}