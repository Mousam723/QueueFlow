package com.queueflow.businessstaff;

import com.queueflow.business.Business;
import com.queueflow.user.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "business_staff",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_business_staff",
                        columnNames = {"business_id", "staff_id"}
                )
        }
)
public class BusinessStaff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private User staff;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Business getBusiness() {
        return business;
    }

    public void setBusiness(Business business) {
        this.business = business;
    }

    public User getStaff() {
        return staff;
    }

    public void setStaff(User staff) {
        this.staff = staff;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
