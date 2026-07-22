package com.meepleday.event

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor

interface EventRepository : JpaRepository<Event, Long>, JpaSpecificationExecutor<Event>
