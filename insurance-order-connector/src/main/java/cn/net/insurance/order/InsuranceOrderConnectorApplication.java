package cn.net.insurance.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication()
@MapperScan({"cn.net.insurance.order.mapper"})
@ComponentScan(value = {"cn.net.insurance"})
@EnableFeignClients(value = {"cn.net.insurance.order"})
@EnableDiscoveryClient
@EnableAsync
public class InsuranceOrderConnectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(InsuranceOrderConnectorApplication.class, args);
    }

}
