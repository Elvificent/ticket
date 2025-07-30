import React from "react";
import "./ComparePlans.css";

const ComparePlans = () => {
  return (
    <div className="compare-clean">
      <h2 className="compare-clean-title">Compare Plans</h2>
      <div className="compare-clean-table">
        <div className="compare-row header">
          <div>Feature</div>
          <div>Manual Support</div>
          <div>Outsourced Support</div>
          <div>Our AI-Enhanced System</div>
        </div>

        <div className="compare-row">
          <div>Query Response</div>
          <div>Slow, human-only</div>
          <div>Variable, often delayed</div>
          <div>Instant with AI assistant</div>
        </div>
        <div className="compare-row">
          <div>Ticket Management</div>
          <div>Spreadsheet-based</div>
          <div>External CRM tools</div>
          <div>Centralized, role-based workflow</div>
        </div>
        <div className="compare-row">
          <div>Engineer Task Dispatch</div>
          <div>Manual, ad hoc</div>
          <div>External teams</div>
          <div>Automated, skill-based routing</div>
        </div>
        <div className="compare-row">
          <div>Skill Management</div>
          <div>Not tracked</div>
          <div>Opaque and generic</div>
          <div>Engineer-specific profiles</div>
        </div>
        <div className="compare-row">
          <div>Customer Data</div>
          <div>Scattered, hard to trace</div>
          <div>Owned by third parties</div>
          <div>Fully centralized & secure</div>
        </div>
        <div className="compare-row">
          <div>Analytics Dashboard</div>
          <div>Limited or none</div>
          <div>Delayed reports</div>
          <div>Real-time KPI tracking</div>
        </div>
        <div className="compare-row">
          <div>Scalability & Control</div>
          <div>Hard to scale</div>
          <div>Low flexibility</div>
          <div>Customizable, in-house</div>
        </div>
      </div>
    </div>
  );
};

export default ComparePlans;
