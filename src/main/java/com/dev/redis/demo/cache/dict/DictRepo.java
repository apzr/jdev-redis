package com.dev.redis.demo.cache.dict;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;


/**
 *
 */
//public interface DictRepo extends PagingAndSortingRepository<Dict, Integer>{
public interface DictRepo extends JpaRepository<Dict, Integer> {

	@Query(value = "SELECT id,name,value "
			+ " FROM t_dict d "
			+ " WHERE id = :id", nativeQuery = true)
	Dict findOne(@Param("id") Integer id);


	@Query(value = "SELECT id,name,value "
			+ " FROM t_dict d "
			+ " ORDER BY id LIMIT :limit ", nativeQuery = true)
	List<Dict> top(@Param("limit") Integer limit);
	
}
