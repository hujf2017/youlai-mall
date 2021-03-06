package com.youlai.auth.service;

import com.youlai.admin.dto.UserDTO;
import com.youlai.admin.api.UserFeignService;
import com.youlai.auth.domain.User;
import com.youlai.common.core.constant.AuthConstants;
import com.youlai.common.core.result.Result;
import com.youlai.common.core.result.ResultCode;
import com.youlai.mall.ums.dto.MemberDTO;
import com.youlai.mall.ums.api.MemberFeignService;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;


/**
 * 自定义用户认证和授权
 */
@Service
@AllArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private UserFeignService userFeignService;
    private MemberFeignService memberFeignService;
    private HttpServletRequest request;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String clientId = request.getParameter("client_id");
        User user = null;
        switch (clientId) {
            case AuthConstants.ADMIN_CLIENT_ID: // 后台用户
                Result<UserDTO> userRes = userFeignService.loadUserByUsername(username, 2);
                if (ResultCode.USER_NOT_EXIST.getCode().equals(userRes.getCode())) {
                    throw new UsernameNotFoundException(ResultCode.USER_NOT_EXIST.getMsg());
                }
                UserDTO userDTO = userRes.getData();
                userDTO.setClientId(clientId);
                user = new User(userDTO);
                break;
            case AuthConstants.WEAPP_CLIENT_ID: // 小程序会员
                Result<MemberDTO> memberRes = memberFeignService.loadMemberByOpenid(username, 1);
                if (ResultCode.USER_NOT_EXIST.getCode().equals(memberRes.getCode())) {
                    throw new UsernameNotFoundException(ResultCode.USER_NOT_EXIST.getMsg());
                }
                MemberDTO memberDTO = memberRes.getData();
                memberDTO.setClientId(clientId);
                user = new User(memberDTO);
                break;
        }
        if (!user.isEnabled()) {
            throw new DisabledException("该账户已被禁用!");
        } else if (!user.isAccountNonLocked()) {
            throw new LockedException("该账号已被锁定!");
        } else if (!user.isAccountNonExpired()) {
            throw new AccountExpiredException("该账号已过期!");
        }
        return user;
    }

}
