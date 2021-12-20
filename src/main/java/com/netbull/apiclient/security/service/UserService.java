package com.netbull.apiclient.security.service;

import com.netbull.apiclient.security.model.LoggedUser;
import com.netbull.apiclient.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private ClientService clientService;

    @Override
    public UserDetails loadUserByUsername(String username){
        LoggedUser user;
        try {
            user  = new LoggedUser(clientService.getClientByEmail(username));
        } catch(Exception e) {
            user = new LoggedUser();
        }
        return user;
    }
}
