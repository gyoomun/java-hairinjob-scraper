package org.nicenomu.hairinjob.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories(basePackages = "org.nicenomu.hairinjob.domain.job.repository")
@EnableTransactionManagement
public class JpaConfig {
}
