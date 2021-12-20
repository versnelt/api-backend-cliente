package com.netbull.apiclient.security.config;

import com.netbull.apiclient.security.utility.JwtAuthenticationEntryPoint;
import com.netbull.apiclient.security.utility.JwtRequestFilter;
import com.netbull.apiclient.utility.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Autowired
    private UserDetailsService userService;

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Autowired
    private StringUtils stringUtils;

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userService).passwordEncoder(stringUtils.getEncoder());
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        http = http.cors().and().csrf().disable();

        http.csrf().disable()
                .authorizeRequests()
                .antMatchers(HttpMethod.POST,"/authenticate", "/v1/clients").permitAll()
                .antMatchers(HttpMethod.GET,"/v1/clients/cpf/**", "/v1/clients/email/**", "/v1/clients").permitAll()
                .antMatchers(HttpMethod.GET,"/v3/api-docs/swagger-config/**", "/api/balance/**",
                        "/swagger-ui-custom.html/**"
                      , "/v3/**", "/swagger-ui.html/**", "/swagger-ui/**"

                        ).permitAll()
                .anyRequest().authenticated()
                .and()
                .exceptionHandling()
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        http.addFilterBefore( jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
    }
}
