package it.gov.innovazione.ndc.alerter.data;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;

@NoRepositoryBean
interface NameableRepository<T, R> extends JpaRepository<T, R> {

    Optional<T> findByName(String name);

    Page<T> findAllBy(Pageable pageable);

}
