package io.dataease.controller.sys;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import io.dataease.auth.annotation.DeLog;
import io.dataease.auth.api.dto.CurrentUserDto;
import io.dataease.commons.constants.SysLogConstants;
import io.dataease.commons.utils.BeanUtils;
import io.dataease.controller.sys.request.UserGridRequest;
import io.dataease.exception.DataEaseException;
import io.dataease.i18n.Translator;
import io.dataease.plugins.common.base.domain.SysRole;
import io.dataease.commons.utils.AuthUtils;
import io.dataease.commons.utils.PageUtils;
import io.dataease.commons.utils.Pager;
import io.dataease.controller.response.ExistLdapUser;
import io.dataease.controller.sys.base.BaseGridRequest;
import io.dataease.controller.sys.request.SysUserCreateRequest;
import io.dataease.controller.sys.request.SysUserPwdRequest;
import io.dataease.controller.sys.request.SysUserStateRequest;
import io.dataease.controller.sys.response.RoleUserItem;
import io.dataease.controller.sys.response.SysUserGridResponse;
import io.dataease.service.sys.SysRoleService;
import io.dataease.service.sys.SysUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@Api(tags = "系统：用户管理")
@ApiSupport(order = 220)
@RequestMapping("/api/user")
public class SysUserController {

    @Resource
    private SysUserService sysUserService;

    @Resource
    private SysRoleService sysRoleService;

    @ApiOperation("查询用户")
    @RequiresPermissions("user:read")
    @PostMapping("/userGrid/{goPage}/{pageSize}")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "goPage", value = "页码", required = true, dataType = "Integer"),
            @ApiImplicitParam(paramType = "path", name = "pageSize", value = "页容量", required = true, dataType = "Integer"),
            @ApiImplicitParam(name = "request", value = "查询条件", required = true)
    })
    public Pager<List<SysUserGridResponse>> userGrid(@PathVariable int goPage, @PathVariable int pageSize,
            @RequestBody UserGridRequest request) {
        Page<Object> page = PageHelper.startPage(goPage, pageSize, true);
        return PageUtils.setPageInfo(page, sysUserService.query(request));
    }

    @ApiIgnore
    @PostMapping("/userLists")
    public List<SysUserGridResponse> userLists(@RequestBody BaseGridRequest request) {
        UserGridRequest userGridRequest = BeanUtils.copyBean(new UserGridRequest(), request);
        return sysUserService.query(userGridRequest);
    }

    @ApiOperation("创建用户")
    @RequiresPermissions("user:add")
    @PostMapping("/create")
    @DeLog(
        operatetype = SysLogConstants.OPERATE_TYPE.CREATE,
        sourcetype = SysLogConstants.SOURCE_TYPE.USER,
        value = "userId"
    )
    public void create(@RequestBody SysUserCreateRequest request) {
        sysUserService.save(request);
    }

    @ApiOperation("更新用户")
    @RequiresPermissions("user:edit")
    @PostMapping("/update")
    @DeLog(
        operatetype = SysLogConstants.OPERATE_TYPE.MODIFY,
        sourcetype = SysLogConstants.SOURCE_TYPE.USER,
        value = "userId"
    )
    public void update(@RequestBody SysUserCreateRequest request) {
        sysUserService.update(request);
    }

    @ApiOperation("删除用户")
    @RequiresPermissions("user:del")
    @PostMapping("/delete/{userId}")
    @ApiImplicitParam(paramType = "path", value = "用户ID", name = "userId", required = true, dataType = "Integer")
    @DeLog(
        operatetype = SysLogConstants.OPERATE_TYPE.DELETE,
        sourcetype = SysLogConstants.SOURCE_TYPE.USER
    )
    public void delete(@PathVariable("userId") Long userId) {
        sysUserService.delete(userId);
    }

    @ApiOperation("更新用户状态")
    @RequiresPermissions("user:edit")
    @RequiresRoles("1")
    @PostMapping("/updateStatus")
    @DeLog(
        operatetype = SysLogConstants.OPERATE_TYPE.MODIFY,
        sourcetype = SysLogConstants.SOURCE_TYPE.USER,
        value = "userId"
    )
    public void updateStatus(@RequestBody SysUserStateRequest request) {
        sysUserService.updateStatus(request);
    }

    @ApiOperation("更新当前用户密码")
    @PostMapping("/updatePwd")
    public void updatePwd(@RequestBody SysUserPwdRequest request) {

        sysUserService.updatePwd(request);
    }

    @ApiOperation("更新指定用户密码")
    @RequiresPermissions("user:editPwd")
    @PostMapping("/adminUpdatePwd")
    public void adminUpdatePwd(@RequestBody SysUserPwdRequest request) {
        sysUserService.adminUpdatePwd(request);
    }

    @ApiOperation("当前用户信息")
    @PostMapping("/personInfo")
    public CurrentUserDto personInfo() {
        CurrentUserDto user = AuthUtils.getUser();
        return user;
    }

    @ApiIgnore
    @ApiOperation("更新个人信息")
    @PostMapping("/updatePersonInfo")
    public void updatePersonInfo(@RequestBody SysUserCreateRequest request) {
        Long userId = AuthUtils.getUser().getUserId();
        // 防止修改他人信息， 防止必填内容留空
        if (!request.getUserId().equals(userId) || request.getEmail() == null || request.getNickName() == null) {
            DataEaseException.throwException(Translator.get("i18n_wrong_content"));
        }
        // 再次验证，匹配格式
        if (StringUtils.isNotBlank(request.getPhone()) && !request.getPhone().matches("^1[3|4|5|7|8][0-9]{9}$")) {
            DataEaseException.throwException(Translator.get("i18n_wrong_tel"));
        }
        if (!request.getEmail().matches("^[a-zA-Z0-9_._-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$")) {
            DataEaseException.throwException(Translator.get("i18n_wrong_email"));
        }
        if (!(2 <= request.getNickName().length() && request.getNickName().length() <= 50)) {
            DataEaseException.throwException(Translator.get("i18n_wrong_name_format"));
        }
        sysUserService.updatePersonBasicInfo(request);
    }

    @ApiOperation("设置语言")
    @PostMapping("/setLanguage/{language}")
    @ApiImplicitParam(paramType = "path", name = "language", value = "语言(zh_CN, zh_TW, en_US)", required = true, dataType = "String")
    public void setLanguage(@PathVariable String language) {
        CurrentUserDto user = AuthUtils.getUser();
        Optional.ofNullable(language).ifPresent(currentLanguage -> {
            if (!currentLanguage.equals(user.getLanguage())) {
                sysUserService.setLanguage(user.getUserId(), currentLanguage);
            }
        });
    }

    @ApiOperation("查询所有角色")
    @PostMapping("/all")
    public List<RoleUserItem> all() {
        return sysRoleService.allRoles();
    }

    @ApiIgnore("查询角色")
    @PostMapping("/roleGrid/{goPage}/{pageSize}")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "goPage", value = "页码", required = true, dataType = "Integer"),
            @ApiImplicitParam(paramType = "path", name = "pageSize", value = "页容量", required = true, dataType = "Integer"),
            @ApiImplicitParam(name = "request", value = "查询条件", required = true)
    })
    public Pager<List<SysRole>> roleGrid(@PathVariable int goPage, @PathVariable int pageSize,
            @RequestBody BaseGridRequest request) {
        Page<Object> page = PageHelper.startPage(goPage, pageSize, true);
        Pager<List<SysRole>> listPager = PageUtils.setPageInfo(page, sysRoleService.query(request));
        return listPager;
    }

    @ApiOperation("已同步用户")
    @PostMapping("/existLdapUsers")
    public List<ExistLdapUser> getExistLdapUsers() {
        List<String> userNames = sysUserService.ldapUserNames();
        return userNames.stream().map(name -> {
            ExistLdapUser ldapUser = new ExistLdapUser();
            ldapUser.setUsername(name);
            return ldapUser;
        }).collect(Collectors.toList());
    }

}
