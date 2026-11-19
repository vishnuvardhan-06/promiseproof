import { useAuth } from "../auth/useAuth";

import CitizenDashboard from "../dashboards/CitizenDashboard";
import ResearcherDashboard from "../dashboards/ResearcherDashboard";
import ReviewerDashboard from "../dashboards/ReviewerDashboard";
import AdminDashboard from "../dashboards/AdminDashboard";

function DashboardPage() {
    const { user } = useAuth();

    switch (user?.role) {
        case "CITIZEN":
            return <CitizenDashboard />;

        case "RESEARCHER":
            return <ResearcherDashboard />;

        case "REVIEWER":
            return <ReviewerDashboard />;

        case "ADMIN":
            return <AdminDashboard />;

        default:
            return <p>Access not available.</p>;
    }
}

export default DashboardPage;
