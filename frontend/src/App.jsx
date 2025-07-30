import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate, Link } from 'react-router-dom';
import Header from './Components/Header';
import Footer from './Components/Footer';

import Introduction from './Pages/Home/Introduction';
import SearchResources from './Pages/Home/SearchResources';
import AboutUs from './Pages/About/AboutUs';
import Chatbot from './Pages/Chatbot/Chatbot';
import CreateTicket from './Pages/Tickets/CreateTicket';
import TicketInformation from './Pages/Tickets/TicketInformation';
import ResourcesAndGuides from './Pages/Resources/ResourcesAndGuides';
import Login from './Pages/Auth/Login';
import Profile from './Pages/Profile/Profile';
import UserManagement from './Pages/Admin/UserManagement';
import ViewLogs from './Pages/Admin/ViewLogs';
import AdminDashboard from './Pages/Admin/AdminDashboard';
import RAGFileManagement from './Pages/Admin/RAGFileManagement';
import AssignedTickets from './Pages/Engineer/AssignedTickets';
import KnowledgeBase from './Pages/Engineer/KnowledgeBase';
import AllTickets from './Pages/Manager/AllTickets';
import ManageEngineers from './Pages/Manager/ManageEngineers';
import ManagerDashboard from './Pages/Manager/ManagerDashboard';
import ManagerTicketDetails from './Pages/Manager/ManagerTicketDetails';
import AssignEngineer from './Pages/Manager/AssignEngineer';
import ForgotPassword from './Pages/ForgotPassword';
import ResetPassword from './Pages/ResetPassword';

import Pricing from './Pages/Pricing/pricing'; //solution testing sherine 

import { AdminRoute, ProtectedRoute, EngineerRoute, ManagerRoute, CustomerOnlyRoute } from './Pages/Tickets/Auth';
import './App.css';

const App = () => {
  return (
    <Router>
      <Header />
      <Routes>
        <Route path="/" element={
          <>
            <Introduction />
            <SearchResources />
            <Footer />
          </>
        } />

        <Route path="/pricing" element={
          <>
            <Pricing />
            <Footer />
          </>
        } />


        <Route path="/about" element={
          <>
            <AboutUs />
            <Footer />
          </>
        } />
            
        {/* Protected routes that require authentication */}
        <Route path="/chatbot" element={
          <CustomerOnlyRoute>
            <Chatbot />
          </CustomerOnlyRoute>
        } />

        <Route path="/view-tickets" element={
          <CustomerOnlyRoute>
            <TicketInformation />
          </CustomerOnlyRoute>
        } />
            
        <Route path="/create-ticket" element={
          <CustomerOnlyRoute>
            <CreateTicket />
          </CustomerOnlyRoute>
        } />
            
        <Route path="/profile" element={
          <ProtectedRoute>
            <Profile />
          </ProtectedRoute>
        } />
            
        {/* Admin routes */}
        <Route path="/admin/dashboard" element={
          <AdminRoute>
            <AdminDashboard />
          </AdminRoute>
        } />
            
        <Route path="/admin/users" element={
          <AdminRoute>
            <UserManagement />
          </AdminRoute>
        } />
          
        <Route path="/admin/logs" element={
          <AdminRoute>
            <ViewLogs />
          </AdminRoute>
        } />
            
        <Route path="/admin/rag-files" element={
          <AdminRoute>
            <RAGFileManagement />
          </AdminRoute>
        } />
            
        {/* Engineer routes (if you have any) */}
        <Route path="/tickets/assigned" element={
          <EngineerRoute>
            <AssignedTickets />
          </EngineerRoute>
        } />

        <Route path="/knowledge-base" element={
          <EngineerRoute>
            <KnowledgeBase />
          </EngineerRoute>
        } />
            
        {/* Manager routes (if you have any) */}
        <Route path="/manager/dashboard" element={
          <ManagerRoute>
            <ManagerDashboard />
          </ManagerRoute>
        } />

        <Route path="/tickets" element={
          <ManagerRoute>
            <AllTickets />
          </ManagerRoute>
        } />

        <Route path="/engineers" element={
          <ManagerRoute>
            <ManageEngineers />
          </ManagerRoute>
        } />

        <Route path="/manager/tickets/:id" element={<ManagerTicketDetails />} />
        <Route path="/manager/tickets/:id/assign" element={<AssignEngineer />} />
            
        <Route path="/resources" element={
          <>
            <ResourcesAndGuides />
            <Footer />
          </>
        } />
        <Route path="/login" element={<Login />} />
        <Route path="/forgot-password" element={<ForgotPassword />} />
        <Route path="/reset-password" element={<ResetPassword />} />
        <Route path="*" element={
          <div className="not-found">
            <h2>Page Not Found</h2>
            <p>The page you're looking for doesn't exist.</p>
            <Link to="/" className="auth-button">Go Home</Link>
          </div>
        } />
      </Routes>
    </Router>
  );
};

export default App;
