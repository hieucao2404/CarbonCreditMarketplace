import { Navigate, useLocation } from "react-router-dom";

const ProtectedRoute = ({children, allowedRoles}) => {
    const token = localStorage.getItem("token");
    const user = JSON.parse(localStorage.getItem("user") || "{}");
    const location = useLocation();

    //No token - redirect to login
    if(!token) {
        return <Navigate to="/login" state={{from: location}} replace />;
    }

    //Has token but wrong role - redirect to unauthorized
    if(allowedRoles && !allowedRoles.includes(user.role)) {
        return <Navigate to="/unauthorized" replace />;
    }

    // Authorized - render the protected component
    return children;
};

export default ProtectedRoute;