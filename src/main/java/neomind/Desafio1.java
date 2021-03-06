package neomind;

import neomind.Control.EntityMan;
import neomind.Control.FornecedorJpaController;
import neomind.Control.exceptions.NonexistentEntityException;
import neomind.Entity.Fornecedor;

import java.util.stream.Collectors;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSet;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;

@WebServlet(name = "desafio1", urlPatterns= {"id","add","delete","update","list","count"}, loadOnStartup = 1)
public class Desafio1 extends HttpServlet {

    EntityMan manager;
    Connection con;
    FornecedorJpaController daoFornecedor;

    @Override
    public void init() throws ServletException {
        boolean notinitialized = false;
        try {
           Class.forName("org.hsqldb.jdbc.JDBCDriver");
        } catch (ClassNotFoundException e) {
           e.printStackTrace(System.out);
        }

        try {
           con=DriverManager.getConnection("jdbc:hsqldb:testDB","SA","");
           ResultSet rs = con.createStatement().executeQuery("select count(*) from Fornecedor");
        } catch (SQLException e) {
           notinitialized = true;
        }
        if(notinitialized) {
           try {
              con=DriverManager.getConnection("jdbc:hsqldb:testDB","SA","");
              con.createStatement().executeUpdate("create table Fornecedor (" +
                                                     " id int generated by default as identity (start with 1)," +
                                                     " name varchar(255) not null," +
                                                     " email varchar(255) not null," +
                                                     " comment varchar(255) not null," +
                                                     " cnpj varchar(255) not null, " +
                                                     " primary key (id) )");
              con.createStatement().executeUpdate("insert into Fornecedor (name,email,comment,cnpj) values " +
                                               "('fornec lorimospm','fornec@loripsom','loreipsum','00.000/0000-00')");
           } catch (SQLException e) {
              e.printStackTrace(System.out);
           }
        }
        try {
           if(con!=null) {
              con.close();
           }
        } catch (SQLException e) {
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
           manager = new EntityMan();
           daoFornecedor = new FornecedorJpaController(manager.getInstance());
        } catch (Exception e) {
           e.printStackTrace(System.out);
        }

        if(daoFornecedor == null) return;
        String userPath = request.getServletPath();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        if(userPath.equals("/count")) {
           Integer total = daoFornecedor.getFornecedorCount();
           response.getWriter().print("{\"count\":"+ total.toString() + "}");
        } else if(userPath.equals("/id")) {
           String idstr = request.getParameter("id");
           Integer idnum = 0;
           if(idstr != null){
              idnum = Integer.parseInt(idstr);
              Fornecedor j = daoFornecedor.findFornecedor(idnum);
              if(j != null) {
                 response.getWriter().print(j.toJson());
              } else {
                 response.getWriter().print("{}");
              }
           }
        } else if(userPath.equals("/list")) {
           String p = request.getParameter("p");
           Integer pnum = 0;
           if(p != null){
              pnum = Integer.parseInt(p);
           }

           int total = daoFornecedor.getFornecedorCount();
           if(total > 0){
              String lista = "[";
              Integer startidx = 10*pnum;
              for(Fornecedor m : daoFornecedor.findFornecedorEntities(10,startidx)){
                 lista = lista + m.toJson() + ",";
              }
              lista = lista.substring(0, lista.length()-1) + "]";
              response.getWriter().print(lista);
           }
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String userPath = request.getServletPath();
        Enumeration parNames = request.getParameterNames();
        String json="";
        daoFornecedor = new FornecedorJpaController(manager.getInstance());

        if(daoFornecedor == null) return;
        Integer countPars = 0;

        while(parNames.hasMoreElements()) {
            countPars ++;
            String item = (String)parNames.nextElement();
            if (item.startsWith("{")) {
               json = item;
            }
            System.out.println("Parameters:"+item);
        }

        try {
            StringBuilder sb = new StringBuilder();
            String s;
            while ((s = request.getReader().readLine()) != null) {
                sb.append(s);
            }
            System.out.println("Receive:"+sb.toString());            
            json = sb.toString();
        } catch (Exception ex) {
            ex.printStackTrace();            
        } 
        

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        if (userPath.equals("/add")) {
           Fornecedor f = new Fornecedor();
           if(json.length() > 2) {  // digest JSON
              f.parseJson(json);
              daoFornecedor.create(f);
              response.getWriter().print("{\"OK\": "+ f.getId().toString() +"}");
           } else {                 // digest DATA
              while(parNames.hasMoreElements())
              {
                 String item = (String)parNames.nextElement();
                 if(item.equals("id")){
                    Integer id = Integer.parseInt(request.getParameter("id"));
                    f.setId(id);
                 }
                 if(item.equals("name")){
                    f.setName(request.getParameter("name"));
                 }
                 if(item.equals("email")){
                    f.setEmail(request.getParameter("email"));
                 }
                 if(item.equals("comment")){
                    f.setComment(request.getParameter("comment"));
                 }
                 if(item.equals("cnpj")){
                    f.setCnpj(request.getParameter("cnpj"));
                 }
              }              
              daoFornecedor.create(f);
              response.getWriter().print("{\"OK\": "+ f.getId().toString() +"}");
           }
        } else if (userPath.equals("/update")) {
           Fornecedor f1 = new Fornecedor();
           if(json.length() > 2) {  // digest JSON
              f1.parseJson(json);
              try {
                 Fornecedor f = daoFornecedor.findFornecedor(f1.getId());
                 f.copy(f1);
                 daoFornecedor.edit(f);
              } catch (Exception e) {
                 response.getWriter().print("{\"Error\": 400 }");
              }
              response.getWriter().print("{\"OK\": "+ f1.getId().toString() +"}");
           } else {                 // digest DATA
              while(parNames.hasMoreElements())
              {
                 String item = (String)parNames.nextElement();
                 if(item.equals("id")){
                    Integer id = Integer.parseInt(request.getParameter("id"));
                    f1.setId(id);
                 }
                 if(item.equals("name")){
                    f1.setName(request.getParameter("name"));
                 }
                 if(item.equals("email")){
                    f1.setEmail(request.getParameter("email"));
                 }
                 if(item.equals("comment")){
                    f1.setComment(request.getParameter("comment"));
                 }
                 if(item.equals("cnpj")){
                    f1.setCnpj(request.getParameter("cnpj"));
                 }
              }
              try {
                 Fornecedor f = daoFornecedor.findFornecedor(f1.getId());
                 f.copy(f1);
                 daoFornecedor.edit(f);
              } catch (Exception e) {
                 response.getWriter().print("{\"Error\": 401 }");
              }
              response.getWriter().print("{\"OK\": "+ f1.getId().toString() +"}");
           }
        } else if (userPath.equals("/delete")) {
           Fornecedor f1 = new Fornecedor();
           Integer id = -1;
           if(json.length() > 2) { // digest JSON
              f1.parseJson(json);
              try {
                 Fornecedor f = daoFornecedor.findFornecedor(f1.getId());
                 f.copy(f1);
                 daoFornecedor.destroy(f.getId());
              } catch (NonexistentEntityException e) {
                 response.getWriter().print("{\"Error\": 300 }");
              }
              response.getWriter().print("{\"OK\": "+ f1.getId().toString() +"}");
           } else {                 // digest DATA
              while(parNames.hasMoreElements())
              {
                 String item = (String)parNames.nextElement();
                 if(item.equals("id")){
                    id = Integer.parseInt(request.getParameter("id"));
                    try {
                       daoFornecedor.destroy(id);
                    } catch (NonexistentEntityException e) {
                       response.getWriter().print("{\"Error\": 301 }");
                    }
                 }
              }
              if (id > -1) {
                 response.getWriter().print("{\"OK\": "+ f1.getId().toString() +"}");
              }
           }
        }
//        String input = null;

//        if ((input = (String) request.getAttribute("com.xp.input")) == null) {
//			String jsonString = request.getReader().lines().collect(Collectors.joining());
//            System.out.println("Request:"+jsonString);
//            request.setAttribute("com.xp.input", jsonString);
//        }       
    }
}
