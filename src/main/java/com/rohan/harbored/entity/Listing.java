package com.rohan.harbored.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.NoArgsConstructor;
import org.springframework.boot.autoconfigure.web.WebProperties;

@NoArgsConstructor
@Entity
public class Listing {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    int id;

    String title;
    String discription;
    String location;
    String city;


    int pricePerMonth;




}

//id, title, description, location, city
//        pricePerMonth
//minStayDays, maxStayDays
//images (list, via Cloudinary)
//owner → User (reference)
//averageRating (derived / cached)
//createdAt
