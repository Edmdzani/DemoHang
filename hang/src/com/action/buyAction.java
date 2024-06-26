package com.action;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
 
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.struts2.ServletActionContext;

import com.dao.THangbanDAO;
import com.dao.TOrderDAO;
import com.dao.TOrderitemDAO;
import com.dao.TUserDAO;
import com.model.THangban;
import com.model.TOrder;
import com.model.TOrderitem;
import com.model.TUser;
import com.opensymphony.xwork2.ActionSupport;
import com.util.Cart;

public class buyAction extends ActionSupport
{
	private String message;
	private String path;
	
	private THangbanDAO hangbanDAO;
	private TOrderDAO orderDAO;
	private TOrderitemDAO orderitemDAO;
	private TUserDAO userDAO;
	
	public String addToCart()
	{
		HttpServletRequest request=ServletActionContext.getRequest();
		HttpSession session=request.getSession();
		Cart cart =(Cart)session.getAttribute("cart");
		
		String id=String.valueOf(new Date().getTime());
		String orderId="";
		//获得客户端发来的请求参数即获得名为times的请求值
		int hangbanId=Integer.parseInt(request.getParameter("hangbanId"));//getParameter获取参数
		String piaoleixing=request.getParameter("piaoleixing");
		int danjia=0;
		if(piaoleixing.equals("成人票")){danjia=hangbanDAO.findById(hangbanId).getChengrenpiaojia();}if(piaoleixing.equals("儿童票")){danjia=hangbanDAO.findById(hangbanId).getErtongpiaojia();}
		
		int shuliang=Integer.parseInt(request.getParameter("shuliang"));
		THangban hangban=hangbanDAO.findById(hangbanId);
		
		TOrderitem orderItem=new TOrderitem();
		
		orderItem.setId(id);
		orderItem.setOrderId(orderId);
		orderItem.setHangbanId(hangbanId);
		orderItem.setPiaoleixing(piaoleixing);

		orderItem.setDanjia(danjia);
		orderItem.setShuliang(shuliang);
		orderItem.setHangban(hangban);
		
		cart.addHangban(id, orderItem);
		
		session.setAttribute("cart", cart);
		
		request.setAttribute("msg", "成功加入购物车");
		return "msg";
	}
	
	
	public String delFromCart()//用户自己删除订单
	{
		HttpServletRequest request=ServletActionContext.getRequest();//获取session对象
		HttpSession session=request.getSession();//在session中获取user对象
		Cart cart =(Cart)session.getAttribute("cart");
		
		cart.delHangban(request.getParameter("id"));
		
        session.setAttribute("cart", cart);
		
		request.setAttribute("msg", "删除完毕");
		return "msg";
	}
	
	
	public String orderAdd()
	{
		HttpServletRequest request=ServletActionContext.getRequest();
		HttpSession session=request.getSession();
		Cart cart =(Cart)session.getAttribute("cart");
		TUser user=(TUser)session.getAttribute("user");
		
		String id=String.valueOf(new Date().getTime());
		
		TOrder order=new TOrder();
		
		order.setId(id);
		order.setUserId(user.getUserId());
		order.setXiadanshi(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()));
		order.setShouhourenming(request.getParameter("shouhourenming"));
		
		order.setShouhourenhua(request.getParameter("shouhourenhua"));
		order.setShouhourenzhi(request.getParameter("shouhourenzhi"));
		order.setZongjiage(cart.getTotalPrice());
		order.setZhuangtai("未受理");
		
		orderDAO.save(order);
		
		for (Iterator it = cart.getItems().values().iterator(); it.hasNext();)
		{
			TOrderitem orderItem = (TOrderitem) it.next();
			orderItem.setOrderId(id);
			orderitemDAO.save(orderItem);
			
			jianqu_piaoshu(orderItem.getHangbanId(),orderItem.getShuliang());
		}
		
		cart.getItems().clear();
		session.setAttribute("cart", cart);
		
		this.setMessage("下单成功");
		this.setPath("hangbanAll.action");
		return "succeed";
		
	}
	
	
	public String orderMana()
	{
		String sql="from TOrder order by zhuangtai";
		List orderList=orderDAO.getHibernateTemplate().find(sql);
		
		for(int i=0;i<orderList.size();i++)
		{
			TOrder order=(TOrder)orderList.get(i);
			order.setUser(userDAO.findById(order.getUserId()));
		}
		
		HttpServletRequest request=ServletActionContext.getRequest();
		request.setAttribute("orderList", orderList);
		return ActionSupport.SUCCESS;
	}
	

	public String orderShouli()
	{
		HttpServletRequest request=ServletActionContext.getRequest();
		String id=request.getParameter("id");
		
		TOrder order=orderDAO.findById(id);//根据主键获得订单
		order.setZhuangtai("已受理");//已经受理订单
		orderDAO.attachDirty(order);//关联对象
		
		request.setAttribute("msg", "订单受理完毕");
		return "msg";
	}
	
	
	public String orderDelAd()//管理员删除订单
	{
		HttpServletRequest request=ServletActionContext.getRequest();
		String id=request.getParameter("id");
		
		TOrder order=orderDAO.findById(id);//获得订单对象
		String sql="delete from TOrderitem where orderId="+order.getId();//拼一个删除订单的SQL
		orderitemDAO.getHibernateTemplate().bulkUpdate(sql);//执行该SQL
		
		orderDAO.delete(order);//删除该订单
		
		request.setAttribute("msg", "订单删除完毕");
		return "msg";
	}
	
	
	public String orderDetail()
	{
		HttpServletRequest request=ServletActionContext.getRequest();
		String orderId=request.getParameter("orderId");
		
		String sql="from TOrderitem where orderId=?";
		Object[] c={orderId};
		List orderitemList=orderitemDAO.getHibernateTemplate().find(sql,c);
		for(int i=0;i<orderitemList.size();i++)
		{
			TOrderitem orderitem=(TOrderitem)orderitemList.get(i);
			orderitem.setHangban(hangbanDAO.findById(orderitem.getHangbanId()));
		}
		request.setAttribute("orderitemList", orderitemList);
		return ActionSupport.SUCCESS;
	}
	
	
	public String orderMine()
	{
		HttpServletRequest request=ServletActionContext.getRequest();
		HttpSession session=request.getSession();
		TUser user=(TUser)session.getAttribute("user");
		
		String sql="from TOrder where userId="+user.getUserId();
		List orderList=orderDAO.getHibernateTemplate().find(sql);
		
		request.setAttribute("orderList", orderList);
		return ActionSupport.SUCCESS;
	}
	
	
	public String orderQuxiao()
	{
		HttpServletRequest request=ServletActionContext.getRequest();
		String id=request.getParameter("id");
		
		TOrder order=orderDAO.findById(id);
		if(order.getZhuangtai().equals("已受理"))
		{
			request.setAttribute("msg", "抱歉，您的订单已经受理，不能取消订单");
		}
		else
		{
			String sql="delete from TOrderitem where orderId="+order.getId();
			orderitemDAO.getHibernateTemplate().bulkUpdate(sql);
			
			orderDAO.delete(order);
			request.setAttribute("msg", "成功取消订单，欢迎您下次购买");
		}
		
		return "msg";
	}
	
	public void jianqu_piaoshu(int hangbanId,int shuliang)
	{
		THangban hangban=hangbanDAO.findById(hangbanId);
		hangban.setShengpiao(hangban.getShengpiao()-shuliang);
		hangbanDAO.attachDirty(hangban);
	}

	public THangbanDAO getHangbanDAO()
	{
		return hangbanDAO;
	}

	public void setHangbanDAO(THangbanDAO hangbanDAO)
	{
		this.hangbanDAO = hangbanDAO;
	}


	public String getMessage()
	{
		return message;
	}


	public void setMessage(String message)
	{
		this.message = message;
	}


	public TUserDAO getUserDAO()
	{
		return userDAO;
	}


	public void setUserDAO(TUserDAO userDAO)
	{
		this.userDAO = userDAO;
	}


	public TOrderDAO getOrderDAO()
	{
		return orderDAO;
	}


	public void setOrderDAO(TOrderDAO orderDAO)
	{
		this.orderDAO = orderDAO;
	}


	public TOrderitemDAO getOrderitemDAO()
	{
		return orderitemDAO;
	}


	public void setOrderitemDAO(TOrderitemDAO orderitemDAO)
	{
		this.orderitemDAO = orderitemDAO;
	}


	public String getPath()
	{
		return path;
	}


	public void setPath(String path)
	{
		this.path = path;
	}
	
}
