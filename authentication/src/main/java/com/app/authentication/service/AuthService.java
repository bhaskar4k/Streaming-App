package com.app.authentication.service;

import com.app.authentication.common.CommonReturn;
import com.app.authentication.common.DbWorker;
import com.app.authentication.entity.TLogExceptions;
import com.app.authentication.entity.TLogin;
import com.app.authentication.entity.TMstUser;
import com.app.authentication.environment.Environment;
import com.app.authentication.jwt.Jwt;
import com.app.authentication.model.JwtUserDetails;
import com.app.authentication.model.TMstUserModel;
import com.app.authentication.repository.TLoginRepository;
import com.app.authentication.security.EncryptionDecryption;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

@Service
@Component
public class AuthService {
    @Autowired
    private TLoginRepository tLoginRepository;
    @Autowired
    private LogExceptionsService logExceptionsService;
    @Autowired
    private Jwt jwt;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private EncryptionDecryption encryptionDecryption;
    private Environment environment;
    private DbWorker dbWorker;
    private String sql_string;
    List<Object> params;

    @PersistenceContext
    private EntityManager entityManager;

    public AuthService(){
        this.encryptionDecryption=new EncryptionDecryption();
        this.environment=new Environment();
        this.dbWorker=new DbWorker();
    }

    public void emitLogoutMessageIntoWebsocket(Long t_mst_user_id, Long device_number) {
        try {
            String device_endpoint = environment.getDeviceEndpoint(t_mst_user_id,device_number);
            messagingTemplate.convertAndSend("/topic/logout"+device_endpoint, CommonReturn.success("Your account has been logged-in from another device. Logging out....", "logout_"+device_endpoint));
        } catch (Exception e) {
            log("emitLogoutMessageIntoWebsocket()",e.getMessage());
            e.printStackTrace();
        }
    }

    @Transactional
    public String generateTokenAndUpdateDB(TMstUserModel new_user, TMstUser validated_user){
        try {
            sql_string = "select count(id) as count from t_login where t_mst_user_id = :value1";
            params = List.of(validated_user.getId());
            Long loggedin_device_number = (Long)dbWorker.getQuery(sql_string, entityManager, params, null).getSingleResult() + 1;

            if(loggedin_device_number>environment.getMaximumLoginDevice()){
                // Generate random integers in range 1 to environment.getMaximum_login_device()
                Random rand = new Random();
                Long removed_device_number = rand.nextLong(environment.getMaximumLoginDevice())+1;

                params = List.of(validated_user.getId(),removed_device_number);
                sql_string = "DELETE FROM t_login WHERE t_mst_user_id = :value1 and device_count = :value2";
                int deleted = dbWorker.getQuery(sql_string, entityManager, params, null).executeUpdate();

                if(deleted==0) return null;

                loggedin_device_number = removed_device_number;

                emitLogoutMessageIntoWebsocket(validated_user.getId(),removed_device_number);
            }

            JwtUserDetails jwt_user_details = new JwtUserDetails(validated_user.getId(),validated_user.getEmail(),validated_user.getIs_subscribed(),new_user.getIp_address(),loggedin_device_number);
            String jwt_token = jwt.generateToken(jwt_user_details);

            if(jwt_token!=null){
                TLogin login_entity = new TLogin(validated_user.getId(),jwt_token,new_user.getIp_address(),loggedin_device_number);
                tLoginRepository.save(login_entity);
            }

            return jwt_token;
        } catch (Exception e) {
            log("generateTokenAndUpdateDB()",e.getMessage());
            return null;
        }
    }

    public String getSubject(String token){
        return jwt.extractSubject(token);
    }

    public Boolean isJwtAuthenticated(String token){
        return jwt.isAuthenticated(token);
    }


    private void log(String function_name, String exception_msg){
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();

        String full_class_path = stackTraceElements[2].getClassName();
        String class_name = full_class_path.substring(full_class_path.lastIndexOf(".") + 1);

        String full_package_path = full_class_path.substring(0, full_class_path.lastIndexOf("."));
        String package_name = full_package_path.substring(full_package_path.lastIndexOf(".") + 1);

        logExceptionsService.saveLogException(new TLogExceptions(package_name,class_name,function_name,exception_msg));
    }
}
