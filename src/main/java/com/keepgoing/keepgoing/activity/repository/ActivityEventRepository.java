package com.keepgoing.keepgoing.activity.repository;

import com.keepgoing.keepgoing.activity.domain.ActivityEvent;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityEventRepository extends JpaRepository<ActivityEvent, Long> {

	public List<ActivityEvent> findByUser_IdAndActivityDateBetween(
			Long userId,
			LocalDate startDate,
			LocalDate endDate
	);

	@Query("""
			select distinct a.activityDate
			from ActivityEvent a
			where a.user.id = :userId
			order by a.activityDate asc
			""")
	List<LocalDate> findDistinctActivityDatesByUserId(@Param("userId") Long userId);
}
