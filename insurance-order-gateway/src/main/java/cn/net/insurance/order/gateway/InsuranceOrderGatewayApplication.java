package cn.net.insurance.order.gateway;

import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceAutoConfigure;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(exclude={DataSourceAutoConfiguration.class, DruidDataSourceAutoConfigure.class})
@ComponentScan(value = {"cn.net.insurance.order"})
@EnableFeignClients(value = {"cn.net.insurance.order"})
@EnableDiscoveryClient
@EnableAsync
public class InsuranceOrderGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(InsuranceOrderGatewayApplication.class, args);
    }

}
