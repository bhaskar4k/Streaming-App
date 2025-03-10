package com.app.dashboard.service;

import com.app.authentication.common.CommonReturn;
import com.app.authentication.common.DbWorker;
import com.app.dashboard.common.Util;
import com.app.dashboard.entity.TLayoutMenu;
import com.app.dashboard.entity.TLogExceptions;
import com.app.dashboard.environment.Environment;
import com.app.dashboard.model.JwtUserDetails;
import com.app.dashboard.repository.TLayoutMenuRepository;
import com.app.dashboard.repository.TLogExceptionsRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.jvnet.hk2.annotations.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Service
@Component
public class DashboardService {
    private Environment environment;
    private Util util;
    @Autowired
    private LogExceptionsService logExceptionsService;
    @Autowired
    private TLayoutMenuRepository tLayoutMenuRepository;
    private DbWorker dbWorker;

    @PersistenceContext
    private EntityManager entityManager;

    private String sql_string;
    List<Object> params;

    public DashboardService(){
        this.environment = new Environment();
        this.util = new Util();
        this.dbWorker=new DbWorker();
    }


    public List<TLayoutMenu> getLayoutMenu(JwtUserDetails user){
        try {
            return tLayoutMenuRepository.findAll();
        } catch (Exception e) {
            log(user.getT_mst_user_id(),"getLayoutMenu()",e.getMessage());
            return null;
        }
    }


    private void log(Long t_mst_user_id, String function_name, String exception_msg){
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();

        String full_class_path = stackTraceElements[2].getClassName();
        String class_name = full_class_path.substring(full_class_path.lastIndexOf(".") + 1);

        String full_package_path = full_class_path.substring(0, full_class_path.lastIndexOf("."));
        String package_name = full_package_path.substring(full_package_path.lastIndexOf(".") + 1);

        logExceptionsService.saveLogException(new TLogExceptions(package_name,class_name,function_name,exception_msg,t_mst_user_id));
    }
}
