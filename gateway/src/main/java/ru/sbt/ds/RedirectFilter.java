package ru.sbt.ds;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 15.05.17.
 */

@Service
public class RedirectFilter extends ZuulFilter {
    @Autowired
    private DiscoveryClient discoveryClient;

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 25;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        RequestContext context = RequestContext.getCurrentContext();

        String serviceId = context.getRequest().getRequestURI().split("/")[1];
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);

        int maxVersion = 0;
        URL url = null;

        for (ServiceInstance instance : instances) {
            Map<String, String> metadata = instance.getMetadata();
            String metainfo = metadata.get("metainfo");
            for (Map.Entry entry : metadata.entrySet()) {
                if (entry.getKey().equals("version") && metainfo.equals("myService")) {
                    Integer intVersion = Integer.valueOf(String.valueOf(entry.getValue()));
                    if (intVersion > maxVersion) {
                        maxVersion = intVersion;
                        try {
                            url = instance.getUri().toURL();
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        if (url != null) {
                context.setRouteHost(url);
        }
        return null;
    }
}
