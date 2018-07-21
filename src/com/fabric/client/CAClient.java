package com.fabric.client;

import com.fabric.network.LoadNetwork;
import com.fabric.participant.UserContext;
import com.fabric.util.Util;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.NetworkConfigurationException;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wrapper class for HFCAClient.class
 * @author Vishal
 */

public class CAClient {

    private HFCAClient hfcaClient;
    private String org;
    private LoadNetwork config;

    /**
     * Constructor - loads the CA configuration from network configuration file and intitialize the caClient for organization org
     * @param org - organization name
     * @throws IOException
     * @throws NetworkConfigurationException
     * @throws InvalidArgumentException
     * @throws org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException
     */
    public CAClient(String org) throws IOException, NetworkConfigurationException, InvalidArgumentException, org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException {
        this.config = new LoadNetwork();
        this.org = org;
        this.hfcaClient = HFCAClient.createNewInstance(config.getCaInfo(org));
    }

    /**
     * Enroll the admin. This admin will be used as a registrar to register other users.
     * @param name - admin name
     * @param secret - admin secret
     * @return adminContext
     * @throws Exception
     */

    public UserContext enrollAdmin(String name, String secret) throws Exception {
        UserContext adminContext;
        adminContext = Util.readUserContext(this.org, name);
        if (adminContext != null) {
            Logger.getLogger(CAClient.class.getName()).log(Level.WARNING, "Admin is already enrolled. Therefore skipping...admin enrollment");
            return adminContext;
        }

        Enrollment enrollment = hfcaClient.enroll(name, secret);
        Logger.getLogger(CAClient.class.getName()).log(Level.INFO, "Admin enrolled.");

        adminContext = new UserContext();
        adminContext.setName(name);
        adminContext.setEnrollment(enrollment);
        adminContext.setAffiliation(config.getOrgInfo(org).getName());
        adminContext.setMspId(config.getOrgInfo(org).getMspId());

        Util.writeUserContext(adminContext);
        return adminContext;
    }

    /**
     * Register and enroll the user with organization MSP provider. User context saved in  /cred directory.
     * This is an admin function; admin should be enrolled before enrolling a user.
     * @param userName
     * @param registrarAdmin - network admin
     * @return UserContext
     * @throws Exception
     */
    public UserContext registerUser(String userName, String registrarAdmin) throws Exception {
        UserContext userContext;
        userContext = Util.readUserContext(this.org, userName);
        if (userContext != null) {
            Logger.getLogger(CAClient.class.getName()).log(Level.WARNING, "UserName - " + userName + "  is already registered. Therefore skipping..... registeration");
            return userContext;
        }
        RegistrationRequest regRequest = new RegistrationRequest(userName, this.org);
        UserContext registrarContext = Util.readUserContext(this.org, registrarAdmin);
        if(registrarContext==null){
            Logger.getLogger(CAClient.class.getName()).log(Level.SEVERE, "Registrar "+registrarAdmin+" is not enrolled. Enroll Registrar.");
            return null;
        }
        String enrollSecret = hfcaClient.register(regRequest,registrarContext);

        Enrollment enrollment = hfcaClient.enroll(userName, enrollSecret);

        userContext = new UserContext();
        userContext.setMspId(config.getOrgInfo(this.org).getMspId());
        userContext.setAffiliation(this.org);
        userContext.setEnrollment(enrollment);
        userContext.setName(userName);

        Util.writeUserContext(userContext);
        Logger.getLogger(CAClient.class.getName()).log(Level.INFO, "UserName - " + userName + "  is successfully registered and enrolled by registrar -  " + registrarAdmin);
        return userContext;
    }

    public  UserContext getUserContext(String userName) throws Exception {
        UserContext userContext;
        userContext = Util.readUserContext(this.org, userName);
        if (userContext != null) {
             return userContext;
        }else{
            Logger.getLogger(CAClient.class.getName()).log(Level.SEVERE, "UserName - " + userName + "  is not enrolled. Register user first.");

        }

    }
}